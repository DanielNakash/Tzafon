#!/bin/zsh
# Builder runner — boots the emulator, runs the daily build headless, then shuts it down.
# Invoked by launchd once/day. Needs the local SDK + keystore.
# Cheap shell gates run FIRST so we don't boot an emulator or spend an AI call for nothing.
set -uo pipefail

REPO="/Users/danielnakash/LocalWorkshop/Tzafon"
INBOX="$REPO/feature-requests/FEATURE_REQUESTS.md"
LOG="$REPO/feature-requests/PIPELINE_LOG.md"
SDK="$HOME/Library/Android/sdk"
LOG_DIR="$REPO/feature-requests/logs"
LOCK="$REPO/feature-requests/.pipeline.lock"   # shared mutex with the groomer
mkdir -p "$LOG_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
ISO="$(date +%Y-%m-%dT%H:%M:%S%z)"   # local time with UTC offset
export ANDROID_HOME="$SDK"
export PATH="$SDK/platform-tools:$SDK/emulator:$SDK/cmdline-tools/latest/bin:$PATH"

cd "$REPO"
source "$REPO/feature-requests/scripts/_lib.sh"

# Automation credentials (git-ignored) — headless auth token, see feature-requests/.env.example.
[ -f "$REPO/feature-requests/.env" ] && source "$REPO/feature-requests/.env"

# --- (0) Serialize: one pipeline job at a time (stale-lock aware). ---
acquire_lock "$LOCK" "BUILD" || exit 0
trap 'rmdir "$LOCK" 2>/dev/null' EXIT

# --- (1) Pipeline paused? ---
if grep -qi '^pipeline: paused' "$INBOX"; then
  echo "$ISO  BUILD  skipped  paused" >> "$LOG"
  exit 0
fi

# --- (2) Any groomed-but-unshipped work at all? (fence-aware; ignores the header example) ---
GROOMED_COUNT="$(awk '/^```/{f=!f; next} !f && /^status:[[:space:]]*groomed[[:space:]]*$/{c++} END{print c+0}' "$INBOX")"
if [ "$GROOMED_COUNT" -eq 0 ]; then
  exit 0    # nothing to build; silent no-op
fi

# --- (3) Release serialization guard (see Q3). Refuse to start a new version while a
#     previously built release is still unmerged. "Merged" == the highest version tag is an
#     ancestor of main. If it isn't, a release branch is awaiting the owner's merge → wait. ---
HIGH_TAG="$(git tag --list 'v*' | sort -V | tail -n1)"
if [ -n "$HIGH_TAG" ] && ! git merge-base --is-ancestor "$HIGH_TAG" main 2>/dev/null; then
  echo "$ISO  BUILD  blocked  unmerged-release $HIGH_TAG (awaiting owner merge)" >> "$LOG"
  exit 0
fi

# --- (4) Boot the AVD headless only if we started it (so we never kill your dev session). ---
STARTED_EMU=0
if ! adb devices | grep -q "emulator-"; then
  "$SDK/emulator/emulator" -avd Pixel_8 -no-window -no-audio -no-boot-anim \
    > "$LOG_DIR/emulator-$STAMP.log" 2>&1 &
  STARTED_EMU=1
  adb wait-for-device
  until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 3
  done
fi

# --- (5) Run the builder headless. ---
echo "$ISO  BUILD  start   groomed-work-found" >> "$LOG"
"$HOME/.local/bin/claude" -p "/build-next-version" \
  --permission-mode acceptEdits \
  > "$LOG_DIR/build-$STAMP.log" 2>&1
BUILD_RC=$?

# --- (6) Only shut down an emulator WE started. ---
if [ "$STARTED_EMU" -eq 1 ]; then
  adb emu kill 2>/dev/null || true
fi

exit $BUILD_RC

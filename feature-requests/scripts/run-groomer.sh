#!/bin/zsh
# Groomer runner — cheap shell gate first; only spends an AI call when there is new work.
# Invoked by launchd hourly.
set -uo pipefail

REPO="/Users/danielnakash/LocalWorkshop/Tzafon"
INBOX="$REPO/feature-requests/FEATURE_REQUESTS.md"
LOG="$REPO/feature-requests/PIPELINE_LOG.md"
LOG_DIR="$REPO/feature-requests/logs"
LOCK="$REPO/feature-requests/.pipeline.lock"   # a directory = atomic mutex (mkdir is atomic)
mkdir -p "$LOG_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
ISO="$(date +%Y-%m-%dT%H:%M:%S%z)"   # local time with UTC offset

cd "$REPO"
source "$REPO/feature-requests/scripts/_lib.sh"

# Automation credentials (git-ignored). launchd agents can't read the login keychain where the
# interactive `claude` stores its OAuth session, so headless runs authenticate via a long-lived
# token in this file (see feature-requests/.env.example). Without it: "Not logged in".
[ -f "$REPO/feature-requests/.env" ] && source "$REPO/feature-requests/.env"

# --- (0) Serialize: never overlap with a builder run or another groomer (stale-lock aware). ---
acquire_lock "$LOCK" "GROOM" || exit 0
trap 'rmdir "$LOCK" 2>/dev/null' EXIT

# --- (a) Pure-shell pre-check. No AI call unless BOTH are true: ---
#     pipeline active  AND  at least one `status: new` request.
if grep -qi '^pipeline: paused' "$INBOX"; then
  echo "$ISO  GROOM  skipped  paused" >> "$LOG"
  exit 0
fi
# Fence-aware count: ignore any `status:` line inside a ``` code fence — the template/example in
# the file header lives in a fence and must NOT be treated as a real request.
NEW_COUNT="$(awk '/^```/{f=!f; next} !f && /^status:[[:space:]]*new[[:space:]]*$/{c++} END{print c+0}' "$INBOX")"
if [ "$NEW_COUNT" -eq 0 ]; then
  # Silent no-op: don't spam the log every hour when there's nothing to do.
  exit 0
fi

# --- (a2) Release serialization guard (mirrors run-builder.sh §3). Never land a groom commit on
#     main while a built release is still awaiting the owner's merge: the release branch was cut
#     from an earlier main, so anything committed after it makes /promote's fast-forward impossible
#     and forces a manual rebase. This is what stalled the v2.8.0 → v2.9.0 handoff. "Merged" == the
#     highest version tag is an ancestor of main. Logged once per stall, not once per hourly run. ---
HIGH_TAG="$(git tag --list 'v*' | sort -V | tail -n1)"
if [ -n "$HIGH_TAG" ] && ! git merge-base --is-ancestor "$HIGH_TAG" main 2>/dev/null; then
  BLOCKED="GROOM  blocked  unmerged-release $HIGH_TAG (awaiting owner merge)"
  tail -n1 "$LOG" | grep -qF "$BLOCKED" || echo "$ISO  $BLOCKED" >> "$LOG"
  exit 0
fi

# --- (b) New work found → wake the AI groomer. ---
echo "$ISO  GROOM  start   new-requests-found" >> "$LOG"
"$HOME/.local/bin/claude" -p "/groom-requests" \
  --permission-mode acceptEdits \
  > "$LOG_DIR/groom-$STAMP.log" 2>&1

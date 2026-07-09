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
ISO="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

cd "$REPO"
source "$REPO/feature-requests/scripts/_lib.sh"

# --- (0) Serialize: never overlap with a builder run or another groomer (stale-lock aware). ---
acquire_lock "$LOCK" "GROOM" || exit 0
trap 'rmdir "$LOCK" 2>/dev/null' EXIT

# --- (a) Pure-shell pre-check. No AI call unless BOTH are true: ---
#     pipeline active  AND  at least one `status: new` request.
if grep -qi '^pipeline: paused' "$INBOX"; then
  echo "$ISO  GROOM  skipped  paused" >> "$LOG"
  exit 0
fi
if ! grep -qE '^status:[[:space:]]*new[[:space:]]*$' "$INBOX"; then
  # Silent no-op: don't spam the log every hour when there's nothing to do.
  exit 0
fi

# --- (b) New work found → wake the AI groomer. ---
echo "$ISO  GROOM  start   new-requests-found" >> "$LOG"
"$HOME/.local/bin/claude" -p "/groom-requests" \
  --permission-mode acceptEdits \
  > "$LOG_DIR/groom-$STAMP.log" 2>&1

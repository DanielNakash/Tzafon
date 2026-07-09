# Shared helpers for the pipeline runners. Sourced, not executed.
# Provides: acquire_lock <lock-dir> <job-name>  — atomic mutex with stale-lock recovery.

# A lock held longer than this (seconds) is assumed dead (crashed run / killed process)
# and is reclaimed. 6h comfortably exceeds a normal build; raise it if your builds run longer.
: "${LOCK_MAX_AGE_SEC:=21600}"

acquire_lock() {
  local lock="$1" job="$2"
  if mkdir "$lock" 2>/dev/null; then
    return 0
  fi
  # Lock exists — is it stale?
  if [ -d "$lock" ]; then
    local mtime now
    mtime="$(stat -f %m "$lock" 2>/dev/null || echo 0)"
    now="$(date +%s)"
    if [ "$((now - mtime))" -gt "$LOCK_MAX_AGE_SEC" ]; then
      echo "$ISO  $job  warn     reclaimed-stale-lock (age $((now - mtime))s)" >> "$LOG"
      rmdir "$lock" 2>/dev/null
      mkdir "$lock" 2>/dev/null && return 0
    fi
  fi
  echo "$ISO  $job  skipped  lock-held" >> "$LOG"
  return 1
}

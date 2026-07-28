#!/bin/zsh
# Account gate for the Tzafon pipeline. Sourced by nothing; run as a command.
#
# Guarantees the groomer and builder only ever run under ONE sanctioned Claude account.
# Two independent facts are pinned by `pin-account.sh` and re-checked on every run:
#
#   1. TOKEN_SHA256 — fingerprint of CLAUDE_CODE_OAUTH_TOKEN in feature-requests/.env. This is
#      the credential the HEADLESS runs actually authenticate with (launchd agents can't read the
#      login keychain), so pinning it is what really decides which account is billed and
#      rate-limited. The interactive login is NOT used by a launchd run.
#
#   2. ACCOUNT_UUID — oauthAccount.accountUuid of the current interactive login (~/.claude.json).
#      Pinning this is what makes "the pipeline follows the account I'm logged into" true: log in
#      as the other account and the pipeline halts instead of running under a mismatched identity.
#
# Fails CLOSED: missing pin file, missing token, logged out, or any mismatch => exit 1.
#
# Usage:  zsh assert-account.sh [JOB]      JOB is a ledger label (GROOM/BUILD); default CHECK.

set -uo pipefail

JOB="${1:-CHECK}"
REPO="/Users/danielnakash/LocalWorkshop/Tzafon"
PIN="$REPO/feature-requests/.pipeline-account"
ENV_FILE="$REPO/feature-requests/.env"
LOG="$REPO/feature-requests/PIPELINE_LOG.md"
ISO="${ISO:-$(date +%Y-%m-%dT%H:%M:%S%z)}"

# Record the reason and fail. Deduped against the last ledger line so an hourly groomer that is
# blocked for a week leaves one line, not 168 (same convention as the unmerged-release guard).
block() {
  local line="$JOB  blocked  wrong-account $1"
  tail -n1 "$LOG" 2>/dev/null | grep -qF "$line" || echo "$ISO  $line" >> "$LOG"
  printf 'account gate: %s\n' "$1" >&2
  exit 1
}

[ -f "$PIN" ] || block "not-pinned (run: zsh feature-requests/scripts/pin-account.sh)"

# Pin file is local machine config, written by pin-account.sh: KEY=VALUE lines only.
ACCOUNT_UUID=""; TOKEN_SHA256=""; ACCOUNT_EMAIL=""
source "$PIN"
[ -n "$ACCOUNT_UUID" ]  || block "pin-file-corrupt (no ACCOUNT_UUID)"
[ -n "$TOKEN_SHA256" ]  || block "pin-file-corrupt (no TOKEN_SHA256)"

# --- 1. The credential a headless run will actually authenticate with. ---
[ -f "$ENV_FILE" ] && source "$ENV_FILE"
TOKEN="${CLAUDE_CODE_OAUTH_TOKEN:-}"
[ -n "$TOKEN" ] || block "no-token (feature-requests/.env has no CLAUDE_CODE_OAUTH_TOKEN)"
NOW_SHA="$(printf '%s' "$TOKEN" | shasum -a 256 | cut -d' ' -f1)"
[ "$NOW_SHA" = "$TOKEN_SHA256" ] || block "token-changed (.env holds a token that is not the pinned one)"

# --- 2. The interactive login must still be the account that owns the pipeline. ---
NOW_UUID="$(plutil -extract oauthAccount.accountUuid raw -o - "$HOME/.claude.json" 2>/dev/null)"
[ -n "$NOW_UUID" ] || block "logged-out (no oauthAccount in ~/.claude.json)"
[ "$NOW_UUID" = "$ACCOUNT_UUID" ] || \
  block "account-switched (logged in as $NOW_UUID, pipeline is pinned to ${ACCOUNT_EMAIL:-$ACCOUNT_UUID})"

exit 0

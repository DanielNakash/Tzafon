#!/bin/zsh
# One-time setup for the pipeline account gate (re-run after rotating the token or deliberately
# changing which account owns the pipeline). Writes feature-requests/.pipeline-account.
#
# PROVENANCE CEREMONY — read this before trusting the gate.
# An OAuth token string carries no readable account identity, so no script can verify offline
# which account minted the token sitting in feature-requests/.env. The only way to be certain it
# belongs to the account you are logged in as is to mint it from that account:
#
#   1. Log in to Claude Code as the account that should own the pipeline.
#   2. Run:  claude setup-token
#   3. Paste the printed token into feature-requests/.env as CLAUDE_CODE_OAUTH_TOKEN.
#   4. Run this script.
#
# After that, assert-account.sh enforces both halves on every run: the token cannot be swapped,
# and the interactive login cannot drift to the other account, without the pipeline halting.

set -uo pipefail

REPO="/Users/danielnakash/LocalWorkshop/Tzafon"
PIN="$REPO/feature-requests/.pipeline-account"
ENV_FILE="$REPO/feature-requests/.env"

die() { printf 'pin-account: %s\n' "$1" >&2; exit 1; }

[ -f "$ENV_FILE" ] || die "no feature-requests/.env — copy .env.example and add the token first."
source "$ENV_FILE"
TOKEN="${CLAUDE_CODE_OAUTH_TOKEN:-}"
[ -n "$TOKEN" ] || die "CLAUDE_CODE_OAUTH_TOKEN is empty in feature-requests/.env."
case "$TOKEN" in
  paste-the-token*) die "feature-requests/.env still holds the placeholder from .env.example." ;;
esac

UUID="$(plutil -extract oauthAccount.accountUuid raw -o - "$HOME/.claude.json" 2>/dev/null)"
EMAIL="$(plutil -extract oauthAccount.emailAddress raw -o - "$HOME/.claude.json" 2>/dev/null)"
[ -n "$UUID" ] || die "not logged in (no oauthAccount in ~/.claude.json) — run: claude login"

SHA="$(printf '%s' "$TOKEN" | shasum -a 256 | cut -d' ' -f1)"

umask 077
cat > "$PIN" <<EOF
# Pinned owner of the Tzafon pipeline. Written by pin-account.sh — do not hand-edit.
# Re-checked on every groomer/builder run by assert-account.sh; any mismatch halts the run.
ACCOUNT_EMAIL=$EMAIL
ACCOUNT_UUID=$UUID
TOKEN_SHA256=$SHA
PINNED_AT=$(date +%Y-%m-%dT%H:%M:%S%z)
EOF

printf 'Pipeline pinned to:\n'
printf '  account : %s\n' "${EMAIL:-<unknown>}"
printf '  uuid    : %s\n' "$UUID"
printf '  token   : sha256:%s… (from %s)\n' "${SHA[1,12]}" "$ENV_FILE"
printf '\nGroomer and builder will now refuse to run under any other account.\n'
printf 'Re-run this script after rotating the token or intentionally switching accounts.\n'

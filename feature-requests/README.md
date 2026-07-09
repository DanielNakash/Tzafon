# Tzafon Feature Pipeline

Turns plain-language feature requests into shipped versions with (almost) zero manual work.

## Your one manual job

Edit [`FEATURE_REQUESTS.md`](FEATURE_REQUESTS.md): append a block, set `status: new`, save. Done.

## What runs automatically

| Job | Cadence | Where | What it does |
|-----|---------|-------|--------------|
| **Groomer** | hourly | local (text-only) | New requests → coded requirements in `v<next-minor> Requirements.md`; flips them to `groomed`. Commits to `main`. |
| **Builder** | daily 03:00 | local (needs SDK + keystore) | Implements the next groomed version on a `release/*` branch, runs tests, builds a signed APK, tags `v<version>`, marks requests `implemented`. **Stops before merging to `main` — you review & merge.** |

Version numbering is automatic: highest `git tag` + minor bump (`v2.0.0` → `2.1.0`).

The logic lives in two slash commands you can also run by hand in an interactive session:
`/groom-requests` and `/build-next-version` (see `.claude/commands/`).

## Install the schedule (one-time)

The scripts call the `claude` CLI at `$HOME/.local/bin/claude` (verified on this machine) — if
`which claude` ever reports a different path, update the two files in `scripts/`.

```sh
cd /Users/danielnakash/LocalWorkshop/Tzafon
chmod +x feature-requests/scripts/*.sh

# Symlink the launchd jobs into place and load them:
ln -sf "$PWD/feature-requests/launchd/com.thefoxworks.tzafon.groomer.plist" ~/Library/LaunchAgents/
ln -sf "$PWD/feature-requests/launchd/com.thefoxworks.tzafon.builder.plist" ~/Library/LaunchAgents/
launchctl load ~/Library/LaunchAgents/com.thefoxworks.tzafon.groomer.plist
launchctl load ~/Library/LaunchAgents/com.thefoxworks.tzafon.builder.plist
```

Run a job on demand: `launchctl start com.thefoxworks.tzafon.groomer`.
Unload: `launchctl unload ~/Library/LaunchAgents/com.thefoxworks.tzafon.<job>.plist`.

> Headless (`claude -p`) can't answer a permission prompt: an un-allow-listed tool is **denied**,
> not queued. The scripts pass `--permission-mode acceptEdits` so file writes/commits proceed, and
> `adb`/`emulator`/`./gradlew`/`git commit`/`git tag` are already allow-listed. If you add a step
> needing a new command, allow-list it first or the headless run will fail safe.

## Controls & safety

- **No idle AI spend:** the hourly groomer is a plain `grep` gate — it only spends an AI call when
  a `status: new` request actually exists. The daily builder likewise greps for `groomed` work (and
  checks the serialization guard) before booting the emulator or calling the model.
- **Pause everything:** set `pipeline: paused` in `FEATURE_REQUESTS.md`. Both jobs no-op.
- **One at a time:** groomer and builder share an atomic lock (`.pipeline.lock`), so they never
  overlap and launchd can't start a second run on top of a long one.
- **Release serialization:** the builder refuses to start version N+1 while version N's release
  branch is still unmerged (highest tag not yet an ancestor of `main`). Merge first, then it proceeds.
- **Emulator courtesy:** the builder only boots/kills an emulator it started itself — it won't kill
  a dev session you already have running.
- **Audit trail:** the permanent record is git history (groom commits list request ids; releases
  are tagged) plus the `status:`/`implemented:` fields in the inbox. `PIPELINE_LOG.md` is a
  git-ignored *local* live tail (so branch/main appends never conflict at merge); full per-run
  transcripts land in `logs/`.
- **Stale-lock recovery:** if a run crashes holding the lock, the next run reclaims it after 6h
  (`LOCK_MAX_AGE_SEC` in `scripts/_lib.sh`).
- **Re-groom safely:** to revise a groomed request, edit it and set `status: new` again — the
  groomer replaces its old requirements section in place instead of duplicating it.
- **Merge gate:** the builder only tags on a `release/*` branch; `main` never moves without you.
- **Fail-safe:** a failed build or red tests leaves requests as `groomed` and does not tag.
- **Philosophy guard:** requests that violate a locked decision are set `rejected`, not built.

## Layout

```
feature-requests/
  FEATURE_REQUESTS.md   ← you edit this
  PIPELINE_LOG.md       ← local live ledger (git-ignored)
  README.md             ← this file
  scripts/              ← launchd runners
  launchd/              ← *.plist job definitions
  logs/                 ← per-run transcripts (git-ignored)
  releases/             ← built APKs (git-ignored via *.apk)
.claude/commands/
  groom-requests.md     ← groomer logic
  build-next-version.md ← builder logic
```

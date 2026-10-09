#!/usr/bin/env bash
set -euo pipefail

# Snapshots parts of this monorepo into the presentation mirrors on GitHub.
# The monorepo stays the source of truth and the deploy target; the mirrors
# are read-only views for showing the project split by concern.
#
# Usage:
#   scripts/mirrors.sh                  sync every mirror
#   scripts/mirrors.sh web db           sync only the named mirrors
#
# Requirements: git and gh, authenticated with write access to the mirrors.

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OWNER="${MIRROR_OWNER:-Juansit-0}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

log() { printf '%s\n' "$*"; }

copy() { # copy <tracked-path> <path-in-snapshot>
  mkdir -p "$WORK/snapshot/$(dirname "$2")"
  cp "$ROOT/$1" "$WORK/snapshot/$2"
}

sync_backend() {
  local file
  while IFS= read -r file; do
    case "$file" in
      web/src/main/resources/static/*) continue ;;
    esac
    copy "$file" "$file"
  done < <(git -C "$ROOT" ls-files -- 'pom.xml' '*/pom.xml' '*/src/main/java/*' '*/src/test/java/*' '*/src/main/resources/*')
  copy .gitignore .gitignore
}

sync_web() {
  local file rel
  while IFS= read -r file; do
    rel="${file#web/src/main/resources/static/}"
    copy "$file" "$rel"
  done < <(git -C "$ROOT" ls-files -- 'web/src/main/resources/static/*')
  while IFS= read -r file; do
    rel="${file#brand/assets/}"
    copy "$file" "assets/$rel"
  done < <(git -C "$ROOT" ls-files -- 'brand/assets/*')
}

sync_db() {
  local file rel
  while IFS= read -r file; do
    rel="${file#adapter-persistence/src/main/resources/db/migration/}"
    copy "$file" "migrations/$rel"
  done < <(git -C "$ROOT" ls-files -- 'adapter-persistence/src/main/resources/db/migration/*')
  copy docs/database.md docs/database.md
  copy docker-compose.yml docker-compose.yml
  copy .env.example .env.example
}

sync_firmware() {
  local file rel
  while IFS= read -r file; do
    rel="${file#firmware/}"
    copy "$file" "$rel"
  done < <(git -C "$ROOT" ls-files -- 'firmware/*')
  copy docs/firmware.md docs/firmware.md
  copy docs/hardware.md docs/hardware.md
}

sync_docs() {
  local file
  while IFS= read -r file; do
    copy "$file" "$file"
  done < <(git -C "$ROOT" ls-files -- 'docs/*' 'brand/*')
  copy PLAN.md PLAN.md
  copy CHANGELOG.md CHANGELOG.md
}

sync_repo() { # sync_repo <name> <readme> <description> <sync-function>
  local name="$1" readme="$2" description="$3" sync="$4"
  log "== $name"
  rm -rf "$WORK/snapshot" "$WORK/repo"
  mkdir -p "$WORK/snapshot"
  "$sync"
  cp "$ROOT/scripts/mirrors/$readme.md" "$WORK/snapshot/README.md"

  if gh repo view "$OWNER/$name" >/dev/null 2>&1; then
    git clone --quiet "https://github.com/$OWNER/$name.git" "$WORK/repo"
    find "$WORK/repo" -mindepth 1 -maxdepth 1 ! -name .git -exec rm -rf {} +
  else
    gh repo create "$OWNER/$name" --public --description "$description" >/dev/null
    git init --quiet "$WORK/repo"
    git -C "$WORK/repo" remote add origin "https://github.com/$OWNER/$name.git"
  fi

  cp -R "$WORK/snapshot/." "$WORK/repo/"
  git -C "$WORK/repo" add -A
  if git -C "$WORK/repo" diff --cached --quiet; then
    log "   no changes"
    return
  fi
  git -C "$WORK/repo" \
    -c user.name="$(git -C "$ROOT" config user.name)" \
    -c user.email="$(git -C "$ROOT" config user.email)" \
    commit --quiet -m "Sync from $OWNER/HayPaComer@$(git -C "$ROOT" rev-parse --short HEAD)"
  git -C "$WORK/repo" branch -M main
  git -C "$WORK/repo" -c credential.helper= -c credential.helper='!gh auth git-credential' push --quiet -u origin main
  log "   https://github.com/$OWNER/$name"
}

requested=("$@")
if [ ${#requested[@]} -eq 0 ]; then
  requested=(backend web db firmware docs)
fi

for mirror in "${requested[@]}"; do
  case "$mirror" in
    backend) sync_repo HayPaComer-backend backend "HayPaComer backend: Java modules, Spring Boot, tests (mirror)" sync_backend ;;
    web) sync_repo HayPaComer-web web "HayPaComer web UI: static ES modules served by Spring (mirror)" sync_web ;;
    db) sync_repo HayPaComer-db db "HayPaComer database: Flyway migrations and schema notes (mirror)" sync_db ;;
    firmware) sync_repo HayPaComer-firmware firmware "HayPaComer firmware: ESP32 sketches for door, temperature, and scale (mirror)" sync_firmware ;;
    docs) sync_repo HayPaComer-docs docs "HayPaComer documentation: architecture, decisions, brand, demo (mirror)" sync_docs ;;
    *) log "unknown mirror: $mirror" >&2; exit 1 ;;
  esac
done

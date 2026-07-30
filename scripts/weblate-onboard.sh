#!/usr/bin/env bash
#
# Onboard one plugin bundle to Weblate (weblate.grounds.gg).
#
# Weblate is the git client: it clones the plugin repo, and when a translator
# finishes a language it commits messages_<lang>.properties to a branch and
# opens a pull request back. There is no config file in the plugin repo and no
# CI sync — a component is a record inside Weblate, created by this one call.
#
# Usage:
#   WEBLATE_API_TOKEN=... scripts/weblate-onboard.sh <repo> <resources-dir> [branch]
#
#   <repo>           repo name under github.com/groundsgg, e.g. plugin-social
#   <resources-dir>  path in that repo holding messages.properties, e.g.
#                    velocity/src/main/resources/gg/grounds/social
#   [branch]         source branch to translate (default: main)
#
# Env:
#   WEBLATE_API_TOKEN  a Weblate token (Settings -> API access of a user in the
#                      Keycloak /admin group, or the onboarding bot user). Required.
#   WEBLATE_API        API base (default https://weblate.grounds.gg/api)
#   WEBLATE_PROJECT    project slug (default grounds); created if absent
#
# One-time prerequisites this script does NOT do (see docs/weblate.md):
#   - a GitHub bot token wired into the Weblate deployment so it can clone the
#     PRIVATE plugin repos and push branches back (grounds-pulumi weblate.ts)
#   - an org webhook github.com/groundsgg -> /hooks/github/ so pushes are seen
#     immediately instead of on Weblate's poll interval
set -euo pipefail

REPO="${1:?usage: weblate-onboard.sh <repo> <resources-dir> [branch]}"
RESDIR="${2:?missing <resources-dir>, e.g. velocity/src/main/resources/gg/grounds/social}"
BRANCH="${3:-main}"

API="${WEBLATE_API:-https://weblate.grounds.gg/api}"
PROJECT="${WEBLATE_PROJECT:-grounds}"
: "${WEBLATE_API_TOKEN:?set WEBLATE_API_TOKEN (Weblate Settings -> API access)}"

RESDIR="${RESDIR%/}"
GIT_URL="https://github.com/groundsgg/${REPO}.git"
AUTH=(-H "Authorization: Token ${WEBLATE_API_TOKEN}")

# Ensure the project exists (idempotent — 400 on a duplicate slug is fine).
if ! curl -fsS "${AUTH[@]}" "${API}/projects/${PROJECT}/" >/dev/null 2>&1; then
  echo "creating project ${PROJECT}"
  curl -fsS "${AUTH[@]}" "${API}/projects/" \
    -F "name=Grounds" -F "slug=${PROJECT}" \
    -F "web=https://grounds.gg/" >/dev/null
fi

# check_flags: the placeholders regex forces every MiniMessage <tag> to survive
#   translation unchanged; java-format guards MessageFormat {0}/{1} args.
# push_branch: MUST be set. Without it Weblate FORKS the repo to push (and
#   forking a private repo needs an administration-scoped token). A named
#   branch makes Weblate push in place and open the PR from there.
# file_format=properties + properties_encoding=utf-8: raw UTF-8, no \uXXXX
#   escaping. Safe because the runtime is Java 9+ (PropertyResourceBundle reads
#   UTF-8 there).
# language_regex excludes en: English is the developer-owned monolingual base.
PAYLOAD=$(cat <<JSON
{
  "name": "${REPO}",
  "slug": "${REPO}",
  "vcs": "github",
  "repo": "${GIT_URL}",
  "push": "${GIT_URL}",
  "branch": "${BRANCH}",
  "push_branch": "weblate",
  "filemask": "${RESDIR}/messages_*.properties",
  "template": "${RESDIR}/messages.properties",
  "new_base": "${RESDIR}/messages.properties",
  "edit_template": false,
  "file_format": "properties",
  "file_format_params": { "properties_encoding": "utf-8" },
  "new_lang": "add",
  "language_regex": "^(?!en\$).+\$",
  "repoweb": "https://github.com/groundsgg/${REPO}/blob/{{branch}}/{{filename}}#L{{line}}",
  "check_flags": "placeholders:r\"<[^>]+>\",java-format"
}
JSON
)

echo "creating component ${PROJECT}/${REPO} <- ${RESDIR}/messages.properties on ${BRANCH}"
RESP=$(curl -fsS "${AUTH[@]}" -H "Content-Type: application/json" \
  "${API}/projects/${PROJECT}/components/" -d "${PAYLOAD}")

# Component creation clones the repo asynchronously; a task_url is returned.
TASK=$(printf '%s' "$RESP" | grep -o '"task_url":"[^"]*"' | cut -d'"' -f4 || true)
echo "created. ${TASK:+track clone: $TASK}"
echo "next: open https://weblate.grounds.gg/projects/${PROJECT}/${REPO}/ and confirm it cloned green."

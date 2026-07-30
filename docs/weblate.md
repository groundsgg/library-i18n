# Translating with Weblate

Translations for this platform are managed in self-hosted **Weblate** at
[weblate.grounds.gg](https://weblate.grounds.gg), running in the core cluster and
deployed by Pulumi (`grounds-pulumi`, `core/src/platform/weblate.ts`). It replaces the
Crowdin pipeline this library was first built against.

The important difference from Crowdin: **Weblate is the git client.** It clones the
plugin repo itself, and when a language is finished it commits the new
`messages_<lang>.properties` to a branch and opens a pull request back. There is no
config file in the plugin repo and no CI sync workflow. A "component" is a record
*inside Weblate*, created once with `scripts/weblate-onboard.sh`.

```
messages.properties (English, developer-owned, in the plugin repo)
   │  push to main
   ▼
GitHub  ──org webhook──▶  Weblate pulls, shows new/changed strings to translators
   ▲                          │ translator finishes a language
   │  pull request            ▼
   └──────────────────  Weblate commits messages_<lang>.properties, opens a PR
                              │ a maintainer reviews + merges
                              ▼
                         the jar now carries the translation
```

Login is Keycloak only (the `grounds` realm); the realm's `/admin` group are Weblate
superusers. There is **no email** — the deployment has no SMTP relay, so Weblate sends
no "new strings" or "your PR merged" mail. Review happens by watching the PR and the
Weblate queue, not an inbox.

## One-time platform setup

Done once for the whole estate, not per plugin. Needs a GitHub org admin and a Weblate
superuser.

1. **A bot GitHub token.** Weblate needs credentials to *clone the private plugin
   repos* (`plugin-social`, `plugin-chat`, `plugin-match`, `plugin-notifications` are
   private) and to *push branches back*. Mint one token on a `groundsgg` machine/bot
   user:
   - classic: `repo` scope; or
   - fine-grained: **Contents: Read+Write** and **Pull requests: Read+Write** on all
     plugin repos.

   One token covers both private-repo read and push-back. `administration`/fork scope
   is **not** needed, because every component sets a `push_branch` and Weblate pushes
   in place instead of forking (see below).

2. **Wire the token into the deployment.** It must reach the Weblate container as
   `WEBLATE_GITHUB_USERNAME` + `WEBLATE_GITHUB_TOKEN` (these populate Weblate's
   `GITHUB_CREDENTIALS` and drive the "GitHub pull request" push method). The token is
   a secret: stage it in the grounds-core Infisical project and surface it into a
   Kubernetes Secret referenced by `weblate.ts` via the same `externalSecretName`
   pattern the OIDC client secret already uses — **not** plaintext `extraConfig`, which
   lands in stored Helm values. *This Pulumi change is not yet made; it is blocked on
   the token in step 1 existing.*

3. **One org webhook.** In `github.com/organizations/groundsgg` → Settings → Webhooks,
   add a **Push** webhook with payload URL `https://weblate.grounds.gg/hooks/github/`,
   content type `application/json`, no secret. The endpoint is anonymous by design —
   Weblate trusts nothing in the payload, it just re-pulls. One org webhook covers
   every plugin repo: Weblate matches the incoming repo URL to whichever component(s)
   use it. Without it, Weblate only notices changes on its slow poll interval.

   ```sh
   gh api -X POST /orgs/groundsgg/hooks -f name=web -F active=true \
     -f 'events[]=push' \
     -f 'config[url]=https://weblate.grounds.gg/hooks/github/' \
     -f 'config[content_type]=json'
   ```

## Onboard a plugin

Once per bundle. `<resources-dir>` is the directory in the plugin repo that holds
`messages.properties` (see the README for where to put it).

```sh
export WEBLATE_API_TOKEN=...   # Weblate Settings -> API access, a /admin-group user
scripts/weblate-onboard.sh plugin-social velocity/src/main/resources/gg/grounds/social
```

That creates (or reuses) the `grounds` project and adds a component with the settings
that matter:

| Setting | Value | Why |
|---|---|---|
| `file_format` | `properties` | Java properties. The old `properties-utf8` id no longer exists. |
| `file_format_params.properties_encoding` | `utf-8` | Raw UTF-8, no `\uXXXX` escaping. Safe on Java 9+. |
| `template` / `new_base` | `<dir>/messages.properties` | The English source is the monolingual base. |
| `filemask` | `<dir>/messages_*.properties` | Where each language lands. |
| `edit_template` | `false` | English is developer-owned; translators cannot edit the source. |
| `new_lang` | `add` | Translators can start a new language themselves. |
| `language_regex` | `^(?!en$).+$` | Don't offer English as a translation of itself. |
| `push_branch` | `weblate` | **Push in place, do not fork.** Without it Weblate forks every repo. |
| `check_flags` | `placeholders:r"<[^>]+>",java-format` | Fail a translation that drops a MiniMessage `<tag>` or breaks a `{0}` arg. |

A repo with **several** bundles (multiple `messages.properties` in different modules)
is better served by the **Component discovery** add-on on one seed component than by N
onboard calls — it creates and syncs a component per bundle automatically.

Worth enabling per component in the Weblate UI:

- **Git commit squashing** — one clean commit per push, so the PR is one diff, not 40.
- **Cleanup translation files** — drop keys from a translation once they leave the
  English base, keeping languages aligned with the source.

## For translators

- Sign in at [weblate.grounds.gg](https://weblate.grounds.gg) with the Grounds
  account button (you must have a `grounds`-realm account — there is no self-signup and
  no invite email).
- Keep every `<tag>` and `{0}` exactly as in the source. Colour and formatting live in
  those tags; dropping one is a failing check, not a style choice.
- You are translating the words between the tags, nothing else.

# library-i18n

Player-facing text as keys, resolved into the language the player actually speaks.

## Why this exists

Today a message is written where it is sent:

```kotlin
source.sendMessage(Component.text("Player not found.", NamedTextColor.RED))
```

That line decides three separate things at once — the wording, the colour, and the
language — and it decides the language as *English, always*. A German client that
announced `de_DE` on login gets English anyway, and the only way to change the wording
is a code change by someone who can build a plugin.

This library separates the three. The call site names a message; the bundle holds the
wording and the colour; Weblate holds the translations.

```kotlin
messages.send(source, SocialMessage.PLAYER_NOT_FOUND)
```

## Using it

**1. Declare the keys.** An enum, so the set of messages a plugin can emit is closed
and every one of them is greppable:

```kotlin
enum class SocialMessage(override val id: String) : MessageKey {
    PLAYER_NOT_FOUND("player.not-found"),
    FRIEND_REQUEST_SENT("friend.request.sent"),
    FRIEND_REQUEST_RECEIVED("friend.request.received"),
}
```

**2. Write the source bundle** at `src/main/resources/gg/grounds/social/messages.properties`.
Values are MiniMessage:

```properties
player.not-found=<red>Player not found.
friend.request.sent=<green>Friend request sent to <target>.
friend.request.received=<green><sender> sent you a friend request. <yellow>Use /friend accept <sender> or /friend deny <sender>.
```

**3. Build one `Translations` per plugin** and send through it:

```kotlin
private val messages = Translations.forBundle("gg.grounds.social.messages", javaClass.classLoader)

messages.send(player, SocialMessage.FRIEND_REQUEST_SENT, "target" to target.username)
```

`render(key, locale, …)` returns the `Component` instead, for lore, inventory titles,
book pages and anything else that is not chat.

### Every system message wears `[Grounds]`

`send` puts the platform's tag in front of the line:

```
[Grounds] You are now friends with Ada!
```

Nothing at the call site opts in. A plugin conforms by doing nothing, which is the only
version of "everyone follows the house style" that survives the tenth plugin — the
alternative is a `PREFIX` constant per repo, which is how chat ended up with `[Party]`,
`[STAFF]` and nothing at all, all at once.

The tag is brand mint on dark grey brackets, and it is deliberately not configurable per
plugin. Colour never bleeds into the message: the line is an uncoloured root with the
prefix and the message as siblings, so a `<red>` error stays red and an uncoloured
message does not turn mint.

Three entry points, and the difference between them is only who is talking:

| Call | Prefixed | For |
| --- | --- | --- |
| `send` | yes | the platform talking to a player — confirmations, errors, announcements |
| `sendUnprefixed` | no | a player talking — chat lines, private messages, staff chat |
| `render` | no | not chat at all — lore, titles, inventory names, book pages |

A bundle that holds only conversation lines can opt out once, at construction, instead of
at every call:

```kotlin
Translations.forBundle(bundle, loader, prefix = Prefix.NONE)
```

Which lines are conversations is `plugin-chat`'s call, not each plugin's: it owns the
channels and defines their shapes. Everything else is the platform speaking and is
prefixed.

### Arguments are unparsed by default

`"target" to name` fills `<target>`. A `String` argument is inserted as **unparsed**
text, so a player named `<red>x</red>` — or, more to the point, a chat message quoted
back into another message — cannot inject markup. Pass a `Component` when formatting
is genuinely intended:

```kotlin
messages.send(player, SocialMessage.FRIEND_REQUEST_SENT,
    "target" to Component.text(target.username, NamedTextColor.AQUA))
```

Placeholder names are MiniMessage tag names: lowercase, `[a-z0-9_-]`.

### Which language a player gets

`Identity.LOCALE` — the locale the client announced. Paper, Velocity and Minestom all
expose it through that same Adventure pointer, so this works identically on all three.

An audience that announced nothing gets the fallback (English). A locale with no
bundle gets the fallback too, and specifically *not* the JVM default locale, which
`ResourceBundle` would otherwise use — that would make a container's `LANG` decide
what players read.

A stored per-player override (`/lang de`) does not exist yet. When it does, it goes in
through `LocaleResolver` without touching any call site:

```kotlin
Translations.forBundle(bundle, loader, localeResolver = LocaleResolver { audience ->
    audience.get(Identity.UUID).map(overrides::get).orElse(null)
        ?: LocaleResolver.FROM_AUDIENCE.localeOf(audience)
})
```

### A missing key is loud, not fatal

A key with no translation in any bundle renders as the key itself and logs once. It is
ugly on purpose: sending nothing would look like a dropped packet to the player and
like nothing at all to us.

## Where the strings live

**In the plugin that sends them, not in this library.** This library is runtime code
only; it ships no bundles.

The alternative — one central bundle for the whole platform — means every wording fix
is a release of this library followed by a version bump in every plugin that depends
on it. Strings change far more often than translation machinery does, so they belong
on the faster release train.

Weblate ([weblate.grounds.gg](https://weblate.grounds.gg)) holds every plugin's source
file in one project, so translators still see a single place to work.

## Weblate wiring

Translations are managed in self-hosted **Weblate**, in the core cluster, deployed by
Pulumi (`grounds-pulumi`, `core/src/platform/weblate.ts`). It replaced Crowdin.

The difference that shapes everything: **Weblate is the git client.** It clones the
plugin repo itself, watches for new English strings, and when a language is finished it
commits `messages_<lang>.properties` to a branch and opens a pull request back. So
there is **no config file in the plugin repo and no CI sync workflow** — a component is
a record inside Weblate, created once:

```sh
export WEBLATE_API_TOKEN=...   # Weblate Settings -> API access
scripts/weblate-onboard.sh plugin-social velocity/src/main/resources/gg/grounds/social
```

Full setup — the one-time platform bits (bot token, org webhook) and the per-plugin
onboarding — is in [`docs/weblate.md`](docs/weblate.md).

Two things are worth knowing here. Weblate pushes translations back as a PR, so a
hand-edited `messages_<lang>.properties` is reconciled by its git client, not a source
of truth — edit English in the repo, everything else in Weblate. And the deployment has
**no SMTP**: Weblate sends no mail at all, so a finished translation announces itself as
a pull request, not an email. Watch the PR, not an inbox.

## Dependency

```kotlin
dependencies {
    implementation("gg.grounds:library-i18n:<version>")
}
```

Adventure and MiniMessage are `compileOnly` here and must come from the platform.
Paper, Velocity and the Minestom runtime each ship their own copy — **do not shade
Adventure into a plugin.** A shaded copy gives you a second `Component` type that the
server's `sendMessage` will not accept.

Compiled against Adventure 4.17.0. Running against a newer Adventure is fine; the
reverse is not, which is why the compile target is deliberately old.

## Not in here, on purpose

- **Plurals.** `"3 friends online"` needs ICU plural rules to be right in Polish or
  Russian, and a `count == 1` shortcut is wrong in a way that only shows up for
  languages nobody on the team reads. Weblate supports ICU MessageFormat; add it when a
  plural string actually appears, not before.
- **Hot reload.** Fixing a typo currently needs a plugin release. If that becomes the
  common case, the shape is a Weblate webhook (or add-on) publishing bundles to object
  storage and a NATS event telling servers to reload — with the classpath bundle
  staying as the fallback. Still no service on the send path.
- **A translation service.** Rendering happens per player, per message, in the chat
  path. A network call there buys nothing that a few kilobytes of properties files in
  memory do not already give.

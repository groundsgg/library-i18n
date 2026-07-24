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
wording and the colour; Crowdin holds the translations.

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

Crowdin holds many source files in one project, so translators still see a single
place to work.

## Crowdin wiring

Two files per plugin, both in `templates/`:

| Copy | To | Does |
|---|---|---|
| `templates/crowdin.yml` | repository root | tells Crowdin which files are sources and where translations land |
| `templates/i18n.yml` | `.github/workflows/i18n.yml` | uploads sources on push, opens a PR with finished translations daily |

The workflow calls the reusable one in this repository, so the Crowdin action is
configured in exactly one place:

```yaml
jobs:
  crowdin:
    uses: groundsgg/library-i18n/.github/workflows/crowdin-sync.yml@main
    secrets: inherit
```

One-time, by hand: create the Crowdin project and set `CROWDIN_PROJECT_ID` and
`CROWDIN_PERSONAL_TOKEN` as **organisation** secrets, so `secrets: inherit` finds
them in every plugin repository.

Translations are never uploaded from the repository, only downloaded into it. A
hand-edited `messages_de.properties` would otherwise overwrite a translator's work.

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
  languages nobody on the team reads. Crowdin exports ICU; add it when a plural
  string actually appears, not before.
- **Hot reload.** Fixing a typo currently needs a plugin release. If that becomes the
  common case, the shape is a Crowdin webhook publishing bundles to object storage and
  a NATS event telling servers to reload — with the classpath bundle staying as the
  fallback. Still no service on the send path.
- **A translation service.** Rendering happens per player, per message, in the chat
  path. A network call there buys nothing that a few kilobytes of properties files in
  memory do not already give.

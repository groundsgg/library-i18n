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
wording and the colour, in English and in every language a player can pick.

```kotlin
messages.send(source, SocialMessage.PLAYER_NOT_FOUND)
```

## Using it

**1. Declare the keys.** An enum, so the set of messages a plugin can emit is closed
and every one of them is greppable:

```kotlin
enum class SocialMessage(
    override val id: String,
    override val level: SystemLevel,
) : SystemMessageKey {
    PLAYER_NOT_FOUND("player.not-found", SystemLevel.ERROR),
    FRIEND_REQUEST_SENT("friend.request.sent", SystemLevel.SUCCESS),
    FRIEND_REQUEST_RECEIVED("friend.request.received", SystemLevel.INFO);

    override val category = SystemCategory.FRIENDS
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
[Grounds] Friends › You are now friends with Ada!
```

Nothing at the call site opts in. A plugin conforms by doing nothing, which is the only
version of "everyone follows the house style" that survives the tenth plugin — the
alternative is a `PREFIX` constant per repo, which is how chat ended up with `[Party]`,
`[STAFF]` and nothing at all, all at once.

The shared layout bundle controls the brand, localized category, separators and placeholder order. The renderer supplies gold (`#E7B84B`), secondary (`#C1B9AA`) and explicit level colours. Message components retain their click/hover events and colours.

Implement `SystemMessageKey` to give a key an explicit `SystemCategory`. Existing `MessageKey` implementations use `GENERAL` context with `INFO` level. `render` stays unprefixed for menus and connection dialogs.

`forBundleWithSystemRenderer` accepts a host callback without changing existing `forBundle` JVM signatures. Use `SystemMessageRenderer.withLogoReadiness` with verified readiness for the actual recipient. Without that callback the output is text only. Do not infer readiness from a pack being sent or accepted.

`sendRendered` wraps already assembled rich content with the same recipient-specific system header. `ChatLayouts.player` supplies the common player layout with sender/message component placeholders; translations may reorder them. Free player text must remain unparsed.

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

### Category prefix

A game or system area (SurvivalGames, Queue, Party, …) names itself instead of
`[Grounds]`:

```kotlin
private val messages =
    Translations.forBundle(
        "gg.grounds.survivalgames.messages",
        javaClass.classLoader,
        prefix = Prefix.category("SurvivalGames"),
    )
```

```properties
grace.ending=The grace period ends in <text><seconds></text> seconds.
fight.begins=<success>The grace period is over. Fight!
```

`send` renders `SurvivalGames › The grace period ends in 10 seconds.`, i.e.
`<accent-deep><bold>SurvivalGames</bold></accent-deep> <faint>›</faint> ` in front of the body.
The body's base colour is `<muted>`, so a bundle line can start with plain text; a line that
sets its own colour (`<success>…`) keeps it. The category line carries no brand and no logo, and
`SystemMessageRenderer` is not involved. `Prefix.category(Component)` takes a styled or
translatable name instead of a string. `sendUnprefixed` and `render` stay bare.

### Colour comes from the palette, not from taste

Bundle values are MiniMessage, so a translator picks the colour. Left alone that means
`<red>`, `<green>`, `<gray>` — sixteen colours from 2011 that have nothing to do with the
Grounds design tokens. `Translations` therefore renders with the semantic tags added:

```properties
player.not-found=<danger>Player not found.
friend.request.sent=<success>Friend request sent to <target>.
friend.list.offline=<muted> (offline)
```

| Tag | Token | Use |
| --- | --- | --- |
| `<text>` | `--text` | body text |
| `<muted>` | `--text-muted` | the message itself, hints, anything subordinate |
| `<faint>` | `--text-faint` | brackets, punctuation |
| `<accent>` | `--accent` | **interactive text only** — links, click-to-run |
| `<accent-deep>` | `--accent-deep` | static brand marks, e.g. the `[Grounds]` tag |
| `<success>` `<warning>` `<danger>` `<info>` | state colours | what they say |

These are the **dark** set of tokens. There is no light set here: a client cannot tell us
what its chat background looks like, and over a Minecraft world there is one right answer.

Two consequences worth knowing. `<danger>` is a warm salmon, not a vivid red — over a
near-black ground the state colours sit at L 0.80, the lowest lightness at which all four
still clear APCA Lc 60, and sRGB has nothing redder up there. And `<accent>` is reserved
for text you can click; the `[Grounds]` tag uses `<accent-deep>` instead, because an
accent on every single line is wallpaper and then nothing reads as the thing to act on.

The standard tags still parse, so no existing bundle breaks. What changed is that `<red>`
now reads as a deviation sitting next to `<danger>`, which is the only enforcement that
survives contact with a translator.

`Palette` also exposes the same colours as `TextColor` constants, for code that builds a
`Component` by hand rather than through a bundle.

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

## Translations

**In the same PR as the English line.** Whoever adds a key to `messages.properties` adds
it to every `messages_<lang>.properties` too — one per language in `SupportedLocales.ALL`
other than English. There is no translation tool and no sync job; the bundles in the
repo are the whole truth, and reviewing a translation is reviewing a diff.

`BundleCheck` keeps that honest. One test per plugin:

```kotlin
@Test
fun `messages are complete`() {
    assertEquals(
        emptyList(),
        BundleCheck.problems("gg.grounds.social.messages", javaClass.classLoader, SocialMessage.entries),
    )
}
```

It fails the build when a `MessageKey` has no English line, when a language file is
missing, lacks a key or keeps one the source dropped, or when a translated line's tags
differ from the English one — a translated `<target>` renders as nothing, and a
forgotten one silently drops the player's name.

Adding a language to `SupportedLocales.ALL` therefore fails every plugin's check until
that plugin translates it, which is the point: a language a player can pick but no
bundle speaks reads as broken.

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
  languages nobody on the team reads. Add ICU MessageFormat when a plural string
  actually appears, not before.
- **Hot reload.** Fixing a typo currently needs a plugin release. If that becomes the
  common case, the shape is CI publishing bundles to object
  storage and a NATS event telling servers to reload — with the classpath bundle
  staying as the fallback. Still no service on the send path.
- **A translation service.** Rendering happens per player, per message, in the chat
  path. A network call there buys nothing that a few kilobytes of properties files in
  memory do not already give.

### Context and level

`SystemCategory` describes the subject (`GENERAL`, `FRIENDS`, `PARTY`, `MATCH`, `STORY`). `SystemLevel` describes its semantic status (`INFO`, `SUCCESS`, `WARNING`, `ERROR`). Typed keys default to `GENERAL` and `INFO`; ordinary `MessageKey` implementations remain supported.

A failed friend operation therefore uses `FRIENDS` with `ERROR`: the visible label stays “Friends”, while the renderer uses the error token for the label. It never infers metadata from message wording or body colours. The body component and its events/styles remain intact. Default layouts show `<category>`; translations can additionally place the localized `<level>` placeholder. Both text and graphical layouts use the same metadata.

```kotlin
enum class FriendsMessage(override val id: String, override val level: SystemLevel) : SystemMessageKey {
    LIST_FULL("friends.list-full", SystemLevel.ERROR);
    override val category = SystemCategory.FRIENDS
}
```

The new chat API is still unreleased; its consumer PRs migrate with it. Existing published `Translations.forBundle` factories are unchanged.

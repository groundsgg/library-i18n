package gg.grounds.i18n

/**
 * One message a player can read, identified by its bundle key.
 *
 * Implement it on an enum so the set of messages a plugin can emit is closed and greppable:
 * ```kotlin
 * enum class SocialMessage(override val id: String) : MessageKey {
 *     FRIEND_REQUEST_SENT("friend.request.sent"),
 *     FRIEND_NOT_FOUND("friend.not-found"),
 * }
 * ```
 *
 * The point of the type is that a key which is not in the bundle is visible as a missing enum
 * constant at the call site, instead of as a raw string that silently renders as itself.
 */
interface MessageKey {
    /** The key as it appears in `messages.properties`. */
    val id: String
}

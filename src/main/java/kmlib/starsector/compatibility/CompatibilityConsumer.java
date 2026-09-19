package kmlib.starsector.compatibility;

import kmlib.text.KmlibStrings;

/**
 * The mod that took a binding to third-party code, as the two things a report needs from it: the
 * identity its records latch under, and the sentence naming what it loses when that binding stops
 * holding.
 *
 * <p>The sentence is the taking mod's rather than the library's, for the reason this package's
 * README sets out.
 *
 * <p>Both travel as one value because a binding site takes them from its caller together and for
 * one purpose, and because two loose strings in a row are two a caller can transpose with nothing
 * to catch it - latching under a whole sentence, and showing a player a key.
 *
 * @param consumerKey the identity a record latches under, spelled once by whoever takes the binding
 *                    and stable for the session; never shown to a player
 * @param lostFeature a whole sentence naming what stops working this session, in the taking mod's
 *                    own wording, for the consequence slot of the modal
 */
public record CompatibilityConsumer(
    String consumerKey,
    String lostFeature) {

    public CompatibilityConsumer {

        KmlibStrings.requireText(
            consumerKey,
            "A consumer with no key could not be told apart from another over the same binding.");
        KmlibStrings.requireText(
            lostFeature,
            "A consumer naming nothing lost would tell the player to worry without saying about what.");
    }
}

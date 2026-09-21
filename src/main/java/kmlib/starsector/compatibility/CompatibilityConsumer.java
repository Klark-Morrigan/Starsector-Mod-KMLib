package kmlib.starsector.compatibility;

import kmlib.starsector.settings.modmanager.InstalledMods;
import kmlib.text.KmlibStrings;

/**
 * The mod that took a binding to third-party code: which mod it is, which of its features the
 * binding serves, and the sentence naming what that feature loses when the binding stops holding.
 *
 * <p>The latch key is composed here rather than supplied whole. A key a caller spelled outright is a
 * key two mods can spell the same, and a record is latched on the pair of third party and consumer -
 * so a collision between two mods puts both behind one latch, and the second mod's player is told
 * what the first one lost or told nothing. Composed from a mod's own ID, that cannot happen between
 * mods: the ID namespaces the key, and a mod that named another's ID would be claiming to be it.
 *
 * <p>The feature half stays the caller's because it is the half nothing else knows. One mod can take
 * two bindings to one third party for two different features and lose two different things by them,
 * and those have to latch apart or the second is dropped - which is the bug the pair latch exists to
 * prevent, at mod granularity instead of subject granularity.
 *
 * <p>The ID is not checked against the game here. That would put a mod-manager read on the healthy
 * path, where this value is built and never looked at again, and there is no honest answer for the
 * moments before the game is up. An ID naming no installed mod instead shows as itself in the
 * report, which is where somebody notices.
 *
 * <p>The sentence is the taking mod's rather than the library's, for the reason this package's
 * README sets out.
 *
 * @param modId             the mod's own ID, as its {@code mod_info.json} declares it, and the half
 *                          of the key no other mod can hold
 * @param featureKey        which of that mod's features took the binding, as a short key, so a mod
 *                          losing two things to one third party is reported twice rather than once
 * @param lostFeature       a whole sentence naming what stops working this session, in the taking
 *                          mod's own wording, for the effect row of the report
 * @param unaffectedFeature a whole sentence naming what goes on working, or null where the mod has
 *                          nothing to add. The taking mod's to write and not the library's, because
 *                          the library cannot promise anything about somebody else's feature or
 *                          somebody else's save - a reassurance written here would be the library
 *                          vouching for a mod it knows nothing about
 */
public record CompatibilityConsumer(
    String modId,
    String featureKey,
    String lostFeature,
    String unaffectedFeature) {

    // What joins the two halves of the key. A colon because no mod ID carries one, so the join
    // cannot be spelled inside either half and collide with a pair nobody registered.
    private static final String KEY_SEPARATOR = ":";

    // How a report names a mod whose display name the game could answer for.
    private static final String NAME_WITH_ID = "%s (%s)";

    public CompatibilityConsumer {

        requireKey(
            modId,
            "A consumer with no mod ID could hold a key another mod holds too.");
        requireKey(
            featureKey,
            "A consumer with no feature key could not be told apart from the same mod's other one.");
        KmlibStrings.requireText(
            lostFeature,
            "A consumer naming nothing lost would tell the player to worry without saying about what.");

        requireSentence(lostFeature);

        if (KmlibStrings.hasText(unaffectedFeature)) {
            requireSentence(unaffectedFeature);
        }
    }

    /**
     * The three-argument form, for a mod with nothing to say about what still works.
     *
     * @param modId       the mod's own ID
     * @param featureKey  which of that mod's features took the binding
     * @param lostFeature the sentence naming what stops working
     */
    public CompatibilityConsumer(String modId, String featureKey, String lostFeature) {

        this(modId, featureKey, lostFeature, null);
    }

    /**
     * @return whether this mod said anything about what a failed binding does not cost, which is
     *         what decides whether the report carries that row at all
     */
    public boolean hasUnaffectedFeature() {

        return KmlibStrings.hasText(unaffectedFeature);
    }

    /**
     * @return the identity a record latches under: the mod and the feature, joined. Never shown to a
     *         player as-is - {@link #describeMod()} is what a report names the mod with
     */
    public String consumerKey() {

        return modId + KEY_SEPARATOR + featureKey;
    }

    /**
     * Names the mod for a report: its own name beside its ID where the game holds one, and the ID
     * alone where it does not.
     *
     * <p>Resolved at report time rather than held, so that the healthy path never reads the mod
     * manager and a report composed before the game is up still names something. An ID the game
     * lists no mod for shows as itself, which is how a misspelled or invented one surfaces.
     *
     * @return the mod as a report names it, never blank
     */
    public String describeMod() {

        var modName = InstalledMods.readModName(modId);

        return modName == null ? modId : String.format(NAME_WITH_ID, modName, modId);
    }

    // The transposition the two key slots cannot catch between themselves: a sentence in a key slot
    // is caught below, and this is a key in a sentence slot. Stated as the shape each slot has
    // rather than as a type, there being no type for either.
    private static void requireSentence(String sentence) {

        if (!sentence.trim().contains(" ")) {
            throw new IllegalArgumentException(
                "A consumer's sentences must read as sentences, not as keys: " + sentence);
        }
    }

    // Non-blank and unbroken by whitespace: what a key is and a sentence is not, which is what makes
    // a sentence handed to a key slot fail here rather than reach a player as a latch.
    private static void requireKey(String key, String whyItMatters) {

        KmlibStrings.requireText(key, whyItMatters);

        if (key.trim().contains(" ")) {
            throw new IllegalArgumentException(whyItMatters + " Got a phrase rather than a key: " + key);
        }
    }
}

package kmlib.starsector.compatibility;

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
 * prevent, at mod granularity instead of subject granularity. A mod that spells one feature key for
 * two features is not refused here, this value being built freely and never checked against what
 * else was built: the record tells the two apart by their sentences and numbers the second's key,
 * through {@link #deconflictFeatureKey}.
 *
 * <p>Plain data: the ID is not checked against the game and no name is looked up for it here. That
 * would put a mod-manager read on the healthy path, where this value is built and never looked at
 * again, and it would make a value object depend on a running game to be read at all. The report
 * resolves the name when it composes one - see {@link CompatibilityFailure#describeMod} - and an ID
 * naming no installed mod shows as itself there, which is where somebody notices.
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

    // What joins a feature key and the number a record tells a reused one apart with. A hyphen
    // rather than the key separator, so the numbered key still reads as one feature key: the mod
    // on one side of the colon, the feature and its number on the other.
    private static final String FEATURE_NUMBER_SEPARATOR = "-";

    // The position under a key that keeps the key bare. A record asks for a numbered key only for
    // the positions after it, and asking for this one would be asking for the key as it stands.
    private static final int FIRST_POSITION = 1;

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
     *         player as-is - {@link CompatibilityFailure#describeMod} is what a report names the
     *         mod with
     */
    public String consumerKey() {

        return modId + KEY_SEPARATOR + featureKey;
    }

    /**
     * This consumer under its feature key numbered, for a record that found the key already
     * holding a different feature of the same mod.
     *
     * <p>The number lands on the feature key because that is what collided: the mod half is the
     * mod's own ID and cannot collide between mods, so two sentences under one key are one mod
     * spelling one feature key twice. Both sentences are that mod's, so the mod and the sentences
     * travel unchanged and only the key is told apart - and a numbered key in the log is the
     * finding, naming the reuse where its author will see it.
     *
     * @param position which record under the reused key this is, counted from one; the first keeps
     *                 the bare key and is never asked for here
     * @return the same mod and sentences under {@code <featureKey>-<position>}
     */
    public CompatibilityConsumer deconflictFeatureKey(int position) {

        if (position <= FIRST_POSITION) {
            throw new IllegalArgumentException(
                "The first record under a key keeps the key bare; only a later one is numbered. Got: " + position);
        }

        return new CompatibilityConsumer(
            modId,
            featureKey + FEATURE_NUMBER_SEPARATOR + position,
            lostFeature,
            unaffectedFeature);
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

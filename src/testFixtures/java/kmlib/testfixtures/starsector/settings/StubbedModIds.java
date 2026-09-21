package kmlib.testfixtures.starsector.settings;

/**
 * The third-party mod IDs a test hands {@link ModStateScopes}, or holds a subject's report against,
 * so the several subjects across the KM mod series that name the same mod are settled against one
 * spelling of it.
 *
 * <p>Stated as literals rather than read off the production constants that carry the same values,
 * which is the whole reason this exists rather than a reference to them: a borrowed constant makes
 * a suite agree with the code by construction, so a rename on the production side would leave both
 * naming an ID the game does not have, and every case still passing. Held apart, a rename fails
 * every case that stubs that mod - which is what a third party changing its ID should do.
 *
 * <p>That only holds while the traffic runs one way. Production code reads its own constant and
 * never this: a class here that the shipped code consulted would be the borrowed constant again,
 * with an extra hop.
 *
 * <p>Published as a fixture variant with the rest of {@code kmlib.testfixtures}, for the reason
 * that package gives - a consuming mod's tests reach it through the fixtures they already compile
 * against, so the IDs are declared once for the series rather than once per suite that stubs one.
 */
public final class StubbedModIds {

    /** Console Commands, which loads the commands KMLib registers with it. */
    public static final String CONSOLE_COMMANDS = "lw_console";

    /** LunaLib, whose settings KMLib binds its own log verbosity to. */
    public static final String LUNALIB = "lunalib";

    /** Nexerelin, whose own colonisation KMLib hands a founding to rather than composing one. */
    public static final String NEXERELIN = "nexerelin";

    /** Random Assortment of Things, whose Abyssal Fracture and mini-map KMLib both read. */
    public static final String RANDOM_ASSORTMENT_OF_THINGS = "assortment_of_things";

    private StubbedModIds() {
    }
}

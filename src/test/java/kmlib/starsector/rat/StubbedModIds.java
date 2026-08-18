package kmlib.starsector.rat;

/**
 * The mod ids the suites in this package hand the mod-state fixture, so the several subjects that
 * gate on the same mod are settled against one spelling of it.
 *
 * <p>Stated as literals rather than read off the production constants, which is the whole reason
 * this is here rather than a reference to them: a rename on the production side would leave a
 * borrowing test agreeing with the code about an id the game does not have, and passing. Held
 * apart, it fails every case that stubs the mod - which is what a rename of a third party's id
 * should do.
 */
final class StubbedModIds {

    static final String RANDOM_ASSORTMENT_OF_THINGS = "assortment_of_things";

    private StubbedModIds() {
    }
}

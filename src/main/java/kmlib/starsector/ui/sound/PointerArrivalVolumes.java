package kmlib.starsector.ui.sound;

/**
 * How loud an arrival is at each kind of thing the pointer can reach - one balance, stated as a whole. The
 * volume half of a {@link UiSoundScheme}'s arrival: the role is named once there, and which level it plays
 * at is answered here per {@link PointerArrivalTarget}.
 *
 * <p>Grouped rather than left as three volumes on the scheme beside the role, on the rule the panel's other
 * look bundles already follow: three bare floats in a row are three positions a caller can transpose with
 * nothing to catch it, and a panel whose chrome answers at its list's level compiles, paints identically,
 * and is wrong only to the ear. Bundled, the swap is confined to one small constructor whose parameters are
 * named a line apart, and a host composing a scheme from settings hands over one value rather than
 * threading three.
 *
 * <p>The three are one balance and not three settings. What makes them right is the ratio between them -
 * quieter for the things a sweep crosses several of than for the ones the player aimed at - so a host that
 * raises one has changed the tuning rather than turned up a part of it. Nothing prevents that and nothing
 * should; it is worth knowing that the numbers are only meaningful together, which is the other reason they
 * travel as one value.
 *
 * @param panelChromeVolume         how loudly reaching the panel's own furniture sounds
 * @param singleOptionControlVolume how loudly reaching a control with one answer to give sounds
 * @param listedItemVolume          how loudly reaching one of many alike sounds
 */
public record PointerArrivalVolumes(
    float panelChromeVolume,
    float singleOptionControlVolume,
    float listedItemVolume) {

    // The balance a KM panel takes when its host states none: vanilla's own arrival level halved for
    // anything the player aims at, and halved again for the items a sweep crosses several of on its way
    // somewhere else. A column of listed rows answering as loudly as the tab above it is what reads as
    // chatter, and the gap between these two numbers is the whole of the fix.
    private static final float DEFAULT_LISTED_ITEM_VOLUME = 0.25f;
    private static final float DEFAULT_PANEL_CHROME_VOLUME = 0.5f;
    private static final float DEFAULT_SINGLE_OPTION_CONTROL_VOLUME = 0.5f;

    /**
     * Rejects a volume the engine has no meaning for at the point the look is composed, rather than leaving
     * it to the cue built from it - which is built on the frame the moment first sounds, where a throw is a
     * crash mid-hover instead of a look that refused to compose.
     */
    public PointerArrivalVolumes {

        UiSoundCue.requirePlayableVolume(panelChromeVolume);
        UiSoundCue.requirePlayableVolume(singleOptionControlVolume);
        UiSoundCue.requirePlayableVolume(listedItemVolume);
    }

    /**
     * The library's own balance, for a host that has no opinion about one yet - which in practice means one
     * with no settings screen to state it from. Shipping a balance rather than demanding numbers keeps a
     * consuming mod from having to invent one before it has a player to tune against; what it gets by
     * saying nothing is what a KM sidebar was tuned at.
     *
     * @return the levels a look takes unless its host names its own
     */
    public static PointerArrivalVolumes createDefaultVolumes() {
        return new PointerArrivalVolumes(
            DEFAULT_PANEL_CHROME_VOLUME,
            DEFAULT_SINGLE_OPTION_CONTROL_VOLUME,
            DEFAULT_LISTED_ITEM_VOLUME);
    }

    /**
     * @param arrivalTarget what kind of thing the pointer reached
     * @return how loudly reaching that kind of thing sounds
     */
    public float resolveVolumeFor(PointerArrivalTarget arrivalTarget) {
        // A switch rather than a map, so a kind added to the enum stops compiling here rather than
        // resolving to a level nobody chose for it.
        return switch (arrivalTarget) {
            case PANEL_CHROME -> panelChromeVolume;
            case SINGLE_OPTION_CONTROL -> singleOptionControlVolume;
            case LISTED_ITEM -> listedItemVolume;
        };
    }
}

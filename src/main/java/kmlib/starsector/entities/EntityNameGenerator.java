package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;

import kmlib.starsector.strings.KmlibStringKeys;
import kmlib.starsector.strings.StarsectorStrings;
import kmlib.text.KmlibNumbers;
import kmlib.text.KmlibStrings;

/**
 * Generates in-universe display names for spawned entities so a dev-created
 * object reads like a charted body on the campaign map rather than debug text.
 * One {@code generate<Kind>Name} method per entity kind keeps each kind's naming
 * convention in a single place.
 */
public final class EntityNameGenerator {

    private EntityNameGenerator() {
    }

    /**
     * Builds the display name for a jump point orbiting {@code focus} at
     * {@code orbitRadius}.
     *
     * <p>Form: the body the point orbits leads, then the localised "Jump-point"
     * word, then the orbital radius in scientific notation - e.g.
     * {@code "Corvus A Jump-point 1.5e3"}. Leading with the focus also reads
     * naturally in single-star systems, where the star is named after its
     * system ({@code "Corvus Jump-point 1.5e3"}). No mod tag is included; that
     * would break immersion. The "Jump-point" word is pulled from KMLib's
     * strings.json (via {@link KmlibStringKeys}) so it stays localisable rather
     * than hard-coded.
     *
     * @param focus       the body the jump point orbits; its name leads the
     *                    label, falling back to its id when it has no name
     * @param orbitRadius the orbit radius, rendered in scientific notation
     * @return the campaign-map display name
     */
    public static String generateJumpPointName(SectorEntityToken focus, float orbitRadius) {
        var jumpPointWord = StarsectorStrings.get(
                KmlibStringKeys.CATEGORY, KmlibStringKeys.JUMP_POINT_LABEL);
        return getLabel(focus) + " " + jumpPointWord + " "
                + KmlibNumbers.formatScientific(orbitRadius);
    }

    // A human-readable focus tag: its name when it has one (stars do), else its
    // id (an invisible barycenter center has no display name).
    private static String getLabel(SectorEntityToken focus) {
        return KmlibStrings.hasText(focus.getName()) ? focus.getName() : focus.getId();
    }
}

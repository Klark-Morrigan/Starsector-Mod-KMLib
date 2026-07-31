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

    // The sentinel vanilla's BaseLocation returns from getName()/getFullName()
    // when an entity has no name of its own (e.g. an abyssal system's invisible
    // center). It is a display placeholder, not a real name, so we treat it as
    // absent and fall through to a better label rather than printing it.
    private static final String VANILLA_UNNAMED_PLACEHOLDER = "unknown location";

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
     * <p>When the focus has no name of its own - an abyssal system's center is an
     * unnamed token - the label falls back to the containing location's name
     * ({@code "The Abyssal Depths Jump-point 1.8e1"}) and finally the focus id.
     *
     * @param focus       the body the jump point orbits; its name leads the
     *                    label, falling back to its location name then its id
     *                    when it has no name of its own
     * @param orbitRadius the orbit radius, rendered in scientific notation
     * @return the campaign-map display name
     */
    public static String generateJumpPointName(SectorEntityToken focus, float orbitRadius) {
        var jumpPointWord = StarsectorStrings.get(
            KmlibStringKeys.CATEGORY,
            KmlibStringKeys.JUMP_POINT_LABEL);
        return getLabel(focus)
            + " "
            + jumpPointWord
            + " "
            + KmlibNumbers.formatScientific(orbitRadius);
    }

    // A human-readable focus tag, preferring the most specific name available:
    // the focus's own name when it has a real one (stars do), else the name of
    // the location that contains it (so an abyssal system's unnamed center still
    // yields the system label, e.g. "The Abyssal Depths"), and finally the focus
    // id as a last resort. Vanilla's "unknown location" placeholder is treated as
    // no name at each step so it never leaks into the label.
    private static String getLabel(SectorEntityToken focus) {
        if (isUsableName(focus.getName())) {
            return focus.getName();
        }
        var location = focus.getContainingLocation();
        if (location != null && isUsableName(location.getName())) {
            return location.getName();
        }
        return focus.getId();
    }

    // A name is usable when it is real text and not vanilla's placeholder for an
    // entity that has none.
    private static boolean isUsableName(String name) {
        return KmlibStrings.hasText(name) && !VANILLA_UNNAMED_PLACEHOLDER.equals(name);
    }
}

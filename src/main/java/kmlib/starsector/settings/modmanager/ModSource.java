package kmlib.starsector.settings.modmanager;

import kmlib.text.KmlibStrings;

import java.util.Objects;

/**
 * One of the places the game reads its data from: a name to show for it, and the mod ID where it is
 * a mod the manager accounts for.
 *
 * <p>Two components rather than a bare name because the two answer different questions. The name is
 * what a player recognises and is always there; the ID is what a command or a lookup takes as an
 * argument, and only a source the mod manager lists has one to give. The base game has no mod ID
 * because it is not a mod, and a folder carrying data that no enabled mod is installed in has none
 * the game could be asked for - inventing one for either would name a mod that does not exist.
 *
 * <p>With {@link ModPresence} rather than with whatever data the source was carrying: what an
 * install has is the mod manager's question, and the answer is the same one whichever kind of data
 * prompted it.
 *
 * @param sourceName what to call the source: a mod's own name, the base game, or the folder a
 *                   source that is neither was read from
 * @param modId      the mod's ID as its {@code mod_info.json} declares it, or null for a source
 *                   that is not an installed mod
 */
public record ModSource(
    String sourceName,
    String modId) {

    public ModSource {
        Objects.requireNonNull(sourceName, "sourceName");
    }

    /**
     * @return true where the source is an installed mod, and so carries an ID worth naming beside
     *         its name
     */
    public boolean hasModId() {
        return KmlibStrings.hasText(modId);
    }
}

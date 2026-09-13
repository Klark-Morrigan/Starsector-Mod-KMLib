package kmlib.starsector.factions;

import kmlib.text.KmlibStrings;

import java.util.Objects;

/**
 * Where a faction was declared: a name to show for the source, and the mod id where the source is
 * one the mod manager accounts for.
 *
 * <p>Two components rather than a bare name because the two answer different questions. The name is
 * what a player recognises and is always there; the id is what another command takes as an
 * argument, and only a source the mod manager lists has one to give. The base game has no mod id
 * because it is not a mod, and a folder carrying data that no enabled mod is installed in has none
 * the game could be asked for - inventing one for either would name a mod that does not exist.
 *
 * @param sourceName what to call the source: a mod's own name, the base game, or the folder a
 *                   source that is neither was read from
 * @param modId      the mod's id as its {@code mod_info.json} declares it, or null for a source
 *                   that is not an installed mod
 */
public record FactionSource(
    String sourceName,
    String modId) {

    public FactionSource {
        Objects.requireNonNull(sourceName, "sourceName");
    }

    /**
     * @return true where the source is an installed mod, and so carries an id worth naming beside
     *         its name
     */
    public boolean hasModId() {
        return KmlibStrings.hasText(modId);
    }
}

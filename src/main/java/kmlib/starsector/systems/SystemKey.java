package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.text.KmlibStrings;

/**
 * What tells one star system apart from another: the ID it answers to, plus the IDs of the two
 * entities the engine builds it around.
 *
 * <p>It exists because {@code StarSystemAPI#getId} is not unique. A modded sector holds several
 * systems sharing an ID - vanilla's own unnamed deep space and abyssal systems among them - and
 * they share their name as well, so neither arm alone tells them apart. Anything keyed on the ID
 * silently keeps the last of a colliding set and drops the rest, which is a system missing from
 * every pass built on that map with nothing anywhere saying so.
 *
 * <p>The three arms are held as fields and compared as such rather than composed into one string,
 * so no separator has to be trusted not to occur inside an entity ID - a composition where
 * {@code a + b} and {@code ab + absent} can meet reintroduces the collision it was meant to break.
 *
 * <p>The anchor arm is the load-bearing one: an anchor's ID is engine-minted per system and
 * persisted in the save as entity identity, so it is both distinct and stable across reloads. The
 * centre arm would not do on its own, since a centre ID may be a literal its creator passed to
 * {@code initStar} and two authors can pick the same one. The ID arm is the collision itself, kept
 * because it is what a person and every external input call the system, and because it is the arm
 * that is never absent.
 *
 * <p>Any arm may be absent - a system need have neither centre nor anchor - and an absent arm is
 * held as the empty string rather than as null, so equality is a comparison of three present
 * values. The key stays discriminating whatever is missing: it separates two systems whenever any
 * arm differs, which makes it at least as telling as its strongest present arm.
 *
 * <p>The one shape that tells nothing apart is the key whose every arm is absent, which the sector
 * states nothing at all about. Two such systems carry equal keys while being two systems, so a
 * caller keying a map on this asks {@link #hasStatedArm} first and holds them apart some other way.
 *
 * <p>The key is internal to code holding systems. A surface addressed from outside - an override
 * table, a persisted preference, a console argument, a log line - stays on the vanilla ID, because
 * that is the only arm a person or a data file can write.
 *
 * @param systemId       the system's own {@code getId}, the name every external input calls it by,
 *                       so a holder of a key can name the system without going back to it
 * @param centreEntityId the ID of the entity the system is built around, empty where it has none
 * @param anchorEntityId the ID of the system's hyperspace anchor, empty where it has none
 */
public record SystemKey(
    String systemId,
    String centreEntityId,
    String anchorEntityId) {

    // What an arm the system does not offer is held as. Empty rather than null so that every
    // comparison, hash and printed line reads three present values, and so a hand-built key cannot
    // fail late on the arm its author had nothing to put in.
    private static final String ABSENT_ARM = "";

    /**
     * Reads an unstated arm as absent, so a key built by hand from partial reads compares the same
     * way as one read off a system.
     */
    public SystemKey {
        systemId = readStatedArm(systemId);
        centreEntityId = readStatedArm(centreEntityId);
        anchorEntityId = readStatedArm(anchorEntityId);
    }

    /**
     * Whether this key states anything at all about the system it was read off.
     *
     * <p>A key with every arm absent equals every other such key, so it identifies nothing: two
     * systems the sector names with neither an ID nor an entity would share one entry of any map
     * keyed on it, which is the very conflation this type exists to prevent. The question is asked
     * here rather than by each keyed map testing three arms for itself.
     *
     * @return true when at least one arm is present, so the key can tell one system from another
     */
    public boolean hasStatedArm() {
        return !(systemId.isEmpty() && centreEntityId.isEmpty() && anchorEntityId.isEmpty());
    }

    /**
     * The key identifying {@code system}, read off the system's own ID and the IDs of its centre
     * and hyperspace anchor.
     *
     * <p>Both entity reads are null-safe: a system may carry neither, and one that carries neither
     * is keyed by its ID alone rather than failing the read.
     *
     * <p>A null system yields null rather than a blank key. A blank key would equal every other
     * blank key, so two callers holding nothing would read as holding the same system - the exact
     * conflation this type exists to prevent.
     *
     * @param system the system to key; null yields null, there being no system to tell apart
     * @return the system's key, or null when {@code system} is null
     */
    public static SystemKey readKeyOf(StarSystemAPI system) {
        if (system == null) {
            return null;
        }
        return new SystemKey(
            system.getId(),
            readEntityId(system.getCenter()),
            readEntityId(system.getHyperspaceAnchor()));
    }

    // An arm as its holder stated it, or the absent token where nothing was stated. The one place
    // absence is decided, so a key read off a system and one built by hand from partial reads
    // cannot disagree about what "not there" is.
    private static String readStatedArm(String arm) {
        return KmlibStrings.hasText(arm) ? arm : ABSENT_ARM;
    }

    // An entity's ID, or nothing where the system carries no such entity - which the constructor
    // then reads as an absent arm, rather than this read naming the absent token itself.
    private static String readEntityId(SectorEntityToken entity) {
        return entity == null ? null : entity.getId();
    }
}

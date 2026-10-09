package kmlib.starsector.factions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;

import kmlib.starsector.SectorWalkCounters;
import kmlib.text.KmlibStrings;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Tells whether the player has settled on a faction identity, and keeps placeholder faction names
 * out of prose.
 *
 * <p>The player faction always reports a display name, but until the player picks one it is a
 * placeholder: vanilla reports {@code "Independent"} before the first colony, and Nexerelin's stock
 * player faction reports the literal {@code "player"}. Substituted into prose, a placeholder reads
 * as a real name.
 *
 * <p>The identity counts as established when the name is not a placeholder OR the player owns a
 * market. Requiring both would misread a custom name chosen before any colony exists, and a default
 * name kept through a real colony.
 *
 * <p>The established check and the display-name fallback read one placeholder set, so the two
 * cannot disagree on what a placeholder is.
 */
public final class StarsectorPlayerFactionResolver {

    // The names the player faction carries before the player names it: vanilla's pre-colony
    // placeholder, and both casings of the literal ID Nexerelin's stock player faction reports.
    private static final Set<String> DEFAULT_UNESTABLISHED_PLACEHOLDERS =
        Set.of("Independent", "player", "Player");

    // Replaced whole on every write and never mutated, so a read sees one complete set or another.
    private static volatile Set<String> unestablishedPlayerFactionNames =
        DEFAULT_UNESTABLISHED_PLACEHOLDERS;

    private StarsectorPlayerFactionResolver() {
    }

    /**
     * Returns the current placeholder set as an unmodifiable view.
     * Callers that want to extend it should read this, build a new set
     * combining the live names with their additions, and pass the
     * result to {@link #setUnestablishedPlayerFactionNames(Set)} - the
     * resolver does not merge for them so the assignment is explicit.
     */
    public static Set<String> getUnestablishedPlayerFactionNames() {
        return Collections.unmodifiableSet(unestablishedPlayerFactionNames);
    }

    /**
     * Returns {@code true} when the player has finalised a faction
     * identity in the running game. See the class doc for the rule.
     */
    public static boolean isPlayerFactionEstablished() {
        return isPlayerFactionEstablished(LiveSource.INSTANCE);
    }

    /**
     * The same question asked of one named sector: whether the player
     * has finalised a faction identity <em>there</em>. Both signals are
     * read off that sector, so a caller drawing a sector the game is
     * not currently running reports what it is looking at rather than
     * what is loaded.
     *
     * @param sector the sector to ask about; no sector holds no player
     *               identity, so it answers {@code false} rather than
     *               throwing
     */
    public static boolean isPlayerFactionEstablished(SectorAPI sector) {

        if (sector == null) {
            return false;
        }

        return isPlayerFactionEstablished(new SectorSource(sector));
    }

    /**
     * Returns {@code faction.getDisplayName()} when it is non-blank
     * and not in the unestablished placeholder set; otherwise returns
     * {@code fallback}. Operates on the supplied {@code faction}
     * (not the player faction specifically) because the same
     * placeholder set tarnishes any faction-display read - host
     * markets, hostile occupiers, etc.
     */
    public static String resolveDisplayName(FactionAPI faction, String fallback) {

        Objects.requireNonNull(fallback, "fallback");

        if (faction == null) {
            return fallback;
        }

        var raw = faction.getDisplayName();
        if (!isEstablishedName(raw)) {
            return fallback;
        }

        return raw;
    }

    /**
     * Replaces the placeholder set, for an environment whose player
     * faction starts under a name the defaults do not know. A
     * {@code null} or empty argument restores the defaults, since an
     * empty set would read every name as established. The set is
     * copied, so later changes to the caller's collection do not reach
     * the resolver.
     */
    public static void setUnestablishedPlayerFactionNames(Set<String> names) {

        if (names == null || names.isEmpty()) {

            unestablishedPlayerFactionNames = DEFAULT_UNESTABLISHED_PLACEHOLDERS;
            return;
        }

        unestablishedPlayerFactionNames =
            Collections.unmodifiableSet(new LinkedHashSet<>(names));
    }

    /**
     * The established check over a source of its two inputs, so the
     * rule can be asked without reaching the game's statics.
     */
    static boolean isPlayerFactionEstablished(PlayerFactionSource source) {

        Objects.requireNonNull(source, "source");

        var playerFaction = source.playerFaction();
        if (playerFaction == null) {
            return false;
        }

        if (isEstablishedName(playerFaction.getDisplayName())) {
            return true;
        }

        return source.ownsAnyMarket();
    }

    // Whether a display name is one the player chose rather than a blank or a placeholder. The one
    // statement of it, so the established check and the display-name fallback cannot drift apart.
    private static boolean isEstablishedName(String name) {

        return KmlibStrings.hasText(name)
            && !unestablishedPlayerFactionNames.contains(name);
    }

    /** Live source backing the public no-arg entry. */
    private enum LiveSource implements PlayerFactionSource {

        INSTANCE;

        @Override
        public FactionAPI playerFaction() {

            return Global.getSector() == null
                ? null
                : Global.getSector().getPlayerFaction();
        }

        @Override
        public boolean ownsAnyMarket() {

            // includeNonPlayerFaction = false: only count markets whose
            // owning faction is literally "player". A Nex commission /
            // governorship reports isPlayerOwned() while still belonging
            // to another faction, which means the player serves under
            // someone else's flag - it does not establish their own
            // faction identity, so it must not satisfy this check.
            return !Misc
                .getPlayerMarkets(false)
                .isEmpty();
        }
    }

    /** The two inputs the established check reads: the player faction, and whether any market is
     *  player-owned. Kept apart from the rule so the rule is a pure function of them. */
    interface PlayerFactionSource {

        FactionAPI playerFaction();

        boolean ownsAnyMarket();
    }

    /** Source over one named sector, backing the sector-bound entry.
     *  Both inputs come off that sector, which is what
     *  {@link LiveSource} cannot offer: {@code Misc.getPlayerMarkets}
     *  reads the sector the game is running, so it answers about the
     *  wrong one whenever the caller is drawing another. */
    private record SectorSource(
        SectorAPI sector)
            implements PlayerFactionSource {

        @Override
        public FactionAPI playerFaction() {

            return sector.getPlayerFaction();
        }

        @Override
        public boolean ownsAnyMarket() {

            var playerFaction = sector.getFaction(Factions.PLAYER);
            var economy = sector.getEconomy();

            // A sector with no economy or no player faction registered
            // has no market to own, which a load in progress can both
            // be true of.
            if (playerFaction == null || economy == null) {
                return false;
            }

            var markets = economy.getMarketsCopy();

            SectorWalkCounters.countMarketsRead(markets.size());

            for (var market : markets) {

                // The test Misc.getPlayerMarkets(false) applies, against
                // this sector's own faction: owned by "player" itself,
                // not merely flown by the player under another flag.
                if (market.getFaction() == playerFaction) {
                    return true;
                }
            }

            return false;
        }
    }
}

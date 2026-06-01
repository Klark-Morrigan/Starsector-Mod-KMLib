package kmlib.starsector.factions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import kmlib.text.KmlibStrings;

/**
 * Faction-display-name plumbing centralised in one place so every
 * tooltip / dialog that has to substitute a faction name shares the
 * same fallback policy.
 *
 * <p>The shape of the problem comes from Starsector's player-faction
 * lifecycle:
 * <ul>
 *   <li>{@code Global.getSector().getPlayerFaction()} always returns
 *       non-null with a non-empty {@code getDisplayName()}, but the
 *       <em>value</em> varies by environment - vanilla pre-first-colony
 *       reports {@code "Independent"}, Nexerelin's stock
 *       {@code player.faction} reports the literal {@code "player"},
 *       and the user can edit either to a custom name later.</li>
 *   <li>Substituting the raw display name into prose ({@code "player
 *       leader in orbit"}, {@code "Production from a local player
 *       settlement..."}) reads poorly when the player has not finalised
 *       an identity yet.</li>
 * </ul>
 *
 * <p>Two surfaces:
 * <ol>
 *   <li>{@link #isPlayerFactionEstablished()} - returns {@code true}
 *       when {@code playerFaction.getDisplayName()} is NOT in the
 *       unestablished-placeholder set OR
 *       {@link Misc#getPlayerMarkets(boolean)} is non-empty. The OR is
 *       deliberate: requiring both signals would mis-classify both
 *       Nex's custom-faction-at-game-start flow (custom name before any
 *       colony exists) and vanilla's keeps-Independent-through-rename
 *       flow (default name on a real identity).</li>
 *   <li>{@link #resolveDisplayName(FactionAPI, String)} - a generic
 *       normaliser callers use against <em>any</em> faction. When the
 *       live display name lands in the placeholder set (or is null /
 *       blank), the caller's context-appropriate fallback is returned
 *       instead.</li>
 * </ol>
 *
 * <p>The placeholder set is shared by both methods so a future change
 * to the recognised default names only needs to land once.
 * Downstream mods can extend it via
 * {@link #setUnestablishedPlayerFactionNames(Set)} when a new launcher
 * environment introduces another placeholder name - the resolver
 * stores a defensive copy so the live set cannot be mutated after the
 * assignment, and a {@code null} or empty argument resets to the
 * built-in defaults rather than silently disabling the check.
 *
 * <p>The public no-arg {@link #isPlayerFactionEstablished} reads live
 * {@code Global} / {@code Misc} state via {@link LiveSource}; a
 * package-private overload accepts an explicit
 * {@link PlayerFactionSource} so callers that already hold the two
 * inputs (player faction, "any player-owned market" flag) can bypass
 * the static reads.
 */
public final class StarsectorPlayerFactionResolver {

    /** Default display-name values that signal the player has not
     *  customised their faction yet (either the pre-first-colony vanilla
     *  placeholder or the literal id casings used by Nexerelin's stock
     *  {@code player.faction} file). Any of these as a live
     *  {@code displayName} reads worse in prose than a context fallback. */
    private static final Set<String> DEFAULT_UNESTABLISHED_PLACEHOLDERS =
            Set.of("Independent", "player", "Player");

    /** Live placeholder set the established-check and the display-name
     *  fallback both read from. Mutable so a downstream mod (or a
     *  LunaLib settings bridge) can extend it once at init when a new
     *  launcher environment introduces another placeholder name, without
     *  needing a KMLib release. Defensive copy on every write so external
     *  collections cannot mutate the live set behind the resolver's
     *  back. */
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
     * Replaces the placeholder set. A {@code null} or empty argument
     * resets the resolver to the built-in defaults rather than
     * silently disabling the unestablished check (an empty set would
     * make every displayName register as established, which defeats
     * the fallback prose the resolver exists for).
     */
    public static void setUnestablishedPlayerFactionNames(Set<String> names) {
        if (names == null || names.isEmpty()) {
            unestablishedPlayerFactionNames = DEFAULT_UNESTABLISHED_PLACEHOLDERS;
            return;
        }
        // Defensive copy so later mutations on the caller's collection
        // do not bleed into the resolver's live set.
        unestablishedPlayerFactionNames =
                Collections.unmodifiableSet(new LinkedHashSet<>(names));
    }

    /**
     * Returns {@code true} when the player has finalised a faction
     * identity. See the class doc for the OR-rule rationale.
     */
    public static boolean isPlayerFactionEstablished() {
        return isPlayerFactionEstablished(LiveSource.INSTANCE);
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
        String raw = faction.getDisplayName();
        if (!KmlibStrings.hasText(raw) || unestablishedPlayerFactionNames.contains(raw)) {
            return fallback;
        }
        return raw;
    }

    /**
     * Package-private overload taking an explicit
     * {@link PlayerFactionSource} so callers that already hold the
     * two inputs can evaluate the established-check without reaching
     * into the static singletons. The public no-arg form delegates
     * here with a {@link LiveSource}.
     */
    static boolean isPlayerFactionEstablished(PlayerFactionSource source) {
        Objects.requireNonNull(source, "source");
        FactionAPI playerFaction = source.playerFaction();
        if (playerFaction == null) {
            return false;
        }
        String name = playerFaction.getDisplayName();
        if (KmlibStrings.hasText(name) && !unestablishedPlayerFactionNames.contains(name)) {
            return true;
        }
        return source.ownsAnyMarket();
    }

    /** Indirection over the two reads {@link #isPlayerFactionEstablished}
     *  needs: the player faction itself, and whether any market is
     *  player-owned. Lets the established-check stay a pure function
     *  of its inputs instead of reaching into static singletons
     *  directly. */
    interface PlayerFactionSource {
        FactionAPI playerFaction();

        boolean ownsAnyMarket();
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
            return !Misc.getPlayerMarkets(false).isEmpty();
        }
    }

}

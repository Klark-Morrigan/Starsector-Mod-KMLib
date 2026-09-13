package kmlib.mods.console.commands;

import kmlib.starsector.markets.colonies.Colony;

import java.util.function.Predicate;

/**
 * Which factions a run lists, and which of a listed faction's holdings its systems clause names.
 * {@link #ALL} is the unfiltered run, the remaining four the keywords the command accepts.
 *
 * <p>Each carries the holdings it selects, so the inclusion test and the systems clause agree by
 * construction: a faction listed for holding a hidden place is one whose clause names the systems
 * those places are in.
 */
enum FactionListingFilter {

    /** No keyword given: every faction, with every holding named. */
    ALL(null, colony -> true),

    /** {@code markets}: a faction holding at least one place of any kind. */
    HOLDS_ANYTHING("markets", colony -> true),

    /** {@code hidden}: a faction holding at least one concealed place. */
    HOLDS_HIDDEN("hidden", Colony::isHidden),

    /** {@code discoverable}: a faction holding at least one undiscovered place. */
    HOLDS_DISCOVERABLE("discoverable", FactionHoldings::isStillDiscoverable),

    /** {@code no_markets}: a faction holding nothing at all. */
    HOLDS_NOTHING("no_markets", colony -> true);

    private final String keyword;
    private final Predicate<Colony> holdingSelection;

    FactionListingFilter(String keyword, Predicate<Colony> holdingSelection) {
        this.keyword = keyword;
        this.holdingSelection = holdingSelection;
    }

    /**
     * @return the bare keyword that selects this filter, or null for the unfiltered run, which has
     *         none to name in the header
     */
    String getKeyword() {
        return keyword;
    }

    Predicate<Colony> getHoldingSelection() {
        return holdingSelection;
    }

    /**
     * Whether a faction holding {@code holdings} belongs in the listing.
     *
     * <p>Counts the holdings this filter selects and answers on that count, so no caller has to
     * know that "which holdings count" and "how many is enough" are two halves of one rule.
     *
     * <p>Exhaustive rather than defaulted on purpose: a filter added to the enum has to state its
     * own rule here, where a default arm would silently give it "at least one" and read as
     * deliberate.
     *
     * @param holdings the places the faction holds
     * @return true when the faction belongs in the listing
     */
    boolean shouldList(FactionHoldings holdings) {

        var matchingCount = holdings.countPlacesMatching(holdingSelection);

        return switch (this) {
            case ALL -> true;
            case HOLDS_NOTHING -> matchingCount == 0;
            case HOLDS_ANYTHING, HOLDS_HIDDEN, HOLDS_DISCOVERABLE -> matchingCount > 0;
        };
    }
}

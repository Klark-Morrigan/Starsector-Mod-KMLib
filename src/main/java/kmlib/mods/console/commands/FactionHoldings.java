package kmlib.mods.console.commands;

import kmlib.starsector.markets.MarketVisibility;
import kmlib.starsector.markets.colonies.Colony;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * The places one faction holds, with the reads a listing poses of them.
 *
 * <p>The same set answers every question one faction's row raises - whether it is listed at all,
 * how many places, how many concealed, how many undiscovered, and in which systems - so it is named
 * once here rather than each question re-walking the sector.
 */
final class FactionHoldings {

    // A colony outside every star system has no ID to name it by. Vanilla builds none, but mods put
    // markets in hyperspace, so the clause says where they are rather than dropping them and
    // under-reporting the systems a faction is in.
    private static final String HYPERSPACE_LABEL = "(hyperspace)";

    private final List<Colony> colonies;

    FactionHoldings(List<Colony> colonies) {
        this.colonies = colonies;
    }

    /**
     * Whether the player has yet to find this colony's body. The negation of the discovery read
     * rather than a read of the entity's own flag, so a colony with no entity at all - nothing left
     * to find - is not reported as findable.
     *
     * @param colony the colony to ask about
     * @return true while the body is still out there to be found
     */
    static boolean isStillDiscoverable(Colony colony) {
        return !MarketVisibility.isDiscoveredByPlayer(colony.market());
    }

    // The distinct systems the selected places sit in, sorted by ID, with hyperspace named once and
    // last - as the sector read itself puts it after the systems, since it is not one and an
    // ordinary ID is what a reader should meet first.
    List<String> collectSystemLabels(Predicate<Colony> selection) {

        var systemIds = new TreeSet<String>();
        var isHyperspaceHeld = false;

        for (var colony : colonies) {

            if (!selection.test(colony)) {
                continue;
            }

            var system = colony.market().getStarSystem();

            if (system == null) {
                isHyperspaceHeld = true;
            } else {
                systemIds.add(system.getId());
            }
        }

        var labels = new ArrayList<String>(systemIds);

        if (isHyperspaceHeld) {
            labels.add(HYPERSPACE_LABEL);
        }
        return labels;
    }

    int countDiscoverablePlaces() {
        return countPlacesMatching(FactionHoldings::isStillDiscoverable);
    }

    int countHiddenPlaces() {
        return countPlacesMatching(Colony::isHidden);
    }

    int countPlaces() {
        return colonies.size();
    }

    int countPlacesMatching(Predicate<Colony> selection) {

        var count = 0;
        for (var colony : colonies) {
            if (selection.test(colony)) {
                count++;
            }
        }
        return count;
    }
}

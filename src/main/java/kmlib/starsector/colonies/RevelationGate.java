package kmlib.starsector.colonies;

/**
 * A shape of colony a bare fog would leak, and the gate that holds it back until somebody has
 * seen it where it stands.
 *
 * <p>Named rather than carried as a boolean apiece because two adjacent booleans are
 * transposable without failing: a rule built with the derelict gate and the concealment gate the
 * wrong way round means something quite different, and compiles, passes and ships. A set of
 * these cannot be got wrong that way, and a third gate becomes one constant here rather than a
 * fourth flag every construction of a rule has to be corrected for.
 *
 * <p>Each gate states what it covers, so the rule applying them asks the gate rather than
 * branching per kind. The two answers differ in nature - one reads what kind of place the colony
 * is, the other reads whether its market conceals itself - which is why neither is derivable
 * from the other, and why a colony can fall under both at once.
 *
 * <p>A gate narrows and never widens. Carrying one holds its shape back until it is revealed;
 * leaving it out drops that shape to the fog alone rather than admitting anything the fog
 * refuses.
 */
public enum RevelationGate {

    /**
     * A derelict nobody ever lived on. The fog admits one the moment its entity is found, and a
     * great many modded stations are never discoverable at all - so ungated, every hulk in the
     * sector is on the map from the first frame of a campaign.
     */
    ABANDONED_STATIONS {

        @Override
        boolean coversColony(Colony colony) {
            return colony.kind() == ColonyKind.ABANDONED_STATION;
        }
    },

    /**
     * A colony that hides itself. Concealment and discovery disagree on these: a colony hidden
     * on an entity that was never discoverable is admitted by the fog on that technicality, so
     * ungated, a secret or wandering colony is shown wherever it currently stands.
     */
    HIDDEN_COLONIES {

        @Override
        boolean coversColony(Colony colony) {
            return colony.isHidden();
        }
    };

    // Whether this gate is about the given colony at all. Package-private because which colonies
    // a gate covers is the visibility rule's own business, and the rule lives beside it - a
    // caller outside has the gate to name, not a colony to test against it.
    abstract boolean coversColony(Colony colony);
}

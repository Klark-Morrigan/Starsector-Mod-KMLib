package kmlib.profiling;

/**
 * One step of the turn a {@link PhasedSection}'s loop takes: what the step is
 * called, the section whose loop it is a step of, and which of that section's
 * tallies it adds to.
 *
 * <p>Handed out by the section rather than named at the call, for the reason a
 * section is a registered value: marking a step inside a per-item loop must cost
 * an array index and an add, or measuring the loop would cost what the loop
 * costs.
 *
 * <p>Knows the section it came from, which is what lets a scope tell a step of
 * its own loop from one belonging to another section's.
 */
public final class ProfilePhase {

    private final PhasedSection section;
    private final String name;
    private final int slotIndex;

    ProfilePhase(PhasedSection section, String name, int slotIndex) {
        this.section = section;
        this.name = name;
        this.slotIndex = slotIndex;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the section whose loop this is a step of
     */
    public PhasedSection getSection() {
        return section;
    }

    /**
     * @return which of its section's phase slots a turn's time is added to here,
     *         fixed when the section declared its phases so that marking one is
     *         not a lookup
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    @Override
    public String toString() {
        return name;
    }
}

package kmlib.profiling.recording;

import kmlib.profiling.ProfileCounter;

import java.util.List;

/**
 * What one counter has been added to during one open scope: what that scope
 * counted itself, and what it counted including everything opened inside it.
 *
 * <p>Two numbers rather than one, because a row reports both. The total is what
 * a parent's row is read for - "this rebuild walked the sector twice", whoever
 * did the walking. The self amount is what the section counted with its own
 * hands, and so the only honest divisor for its self time.
 *
 * <p>Kept per open scope rather than added straight to the tree, because a
 * row's per-call spread is a fact about one call and can only be known once
 * that call has ended.
 */
final class ScopeCount {

    private final ProfileCounter counter;

    private long selfAmount;
    private long totalAmount;

    ScopeCount(ProfileCounter counter) {
        this.counter = counter;
    }

    /**
     * Finds the tally {@code counter} keeps among {@code counts}, appending one
     * the first time that counter is added to in a scope.
     *
     * @param counts  the tallies one open scope has so far, in first-added order
     * @param counter the counter being added to
     * @return the tally that scope keeps for the counter
     */
    static ScopeCount resolveCountIn(List<ScopeCount> counts, ProfileCounter counter) {
        return IdentityLookup.resolveByKey(
            counts, ScopeCount::getCounter, counter, ScopeCount::new);
    }

    ProfileCounter getCounter() {
        return counter;
    }

    long getSelfAmount() {
        return selfAmount;
    }

    long getTotalAmount() {
        return totalAmount;
    }

    /**
     * Adds what this scope counted itself, which is part of its total too.
     */
    void addSelfAmount(long amount) {
        selfAmount += amount;
        totalAmount += amount;
    }

    /**
     * Adds what a scope opened inside this one counted: part of this scope's
     * total, and none of what its own self time is spread over.
     */
    void addChildAmount(long amount) {
        totalAmount += amount;
    }
}

package kmlib.profiling;

/**
 * An open section: work runs inside it and counts what it handled, and closing
 * it records both under whatever scope was open when this one was opened.
 *
 * <p>{@link AutoCloseable} so a caller opens it in try-with-resources and the
 * language closes it - including down a throwing path, which is where a
 * hand-written close is forgotten and the tree keeps a branch that has already
 * returned. Closing throws nothing, so nesting one costs no catch.
 */
public interface ProfileScope extends AutoCloseable {

    /**
     * Adds {@code amount} to what this scope has counted of {@code counter} -
     * the systems it visited, the markets it read, the walks it performed.
     *
     * <p>Counts roll up the way time does: the amount is part of this scope's
     * row and of every row this scope is open inside, so a rebuild's row states
     * how much was counted beneath it whoever counted it.
     *
     * @param counter what is being counted
     * @param amount  how many to add to what this call has counted already
     */
    void addCount(ProfileCounter counter, long amount);

    /**
     * Ends this scope and records its span. Closing an already-closed scope
     * records nothing, so a caller that closes both explicitly and by
     * try-with-resources is not counted twice.
     */
    @Override
    void close();
}

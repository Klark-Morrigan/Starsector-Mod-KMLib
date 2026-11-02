package kmlib.cache.revisions;

import java.util.Arrays;

/**
 * The ordered revisions a reading was taken under, compared by content, so a holder can tell
 * whether anything the reading depends on has moved since.
 *
 * <p>Kept whole rather than folded into one int. A fold maps two different sets of revisions to
 * one number now and then, and a holder comparing folds skips the rebuild whenever that happens.
 * A stamp equals another only when every revision does, in the same order.
 *
 * <p>Order is part of the content, and so is length. A caller joining a stamp of fixed length to
 * one whose length varies {@linkplain #append appends} the variable one last: two stamps split
 * from one sequence at different points would otherwise join to equal stamps.
 *
 * <p>A holder keeping one value resolved under a stamp takes {@link kmlib.cache.memo.RevisionMemo};
 * a holder deciding something else off it, such as whether to rebuild, compares stamps itself.
 */
public final class RevisionStamp {

    /**
     * The stamp of a reading that samples nothing live, which never moves.
     */
    public static final RevisionStamp EMPTY = new RevisionStamp(new int[0]);

    private final int[] revisions;

    private RevisionStamp(int[] revisions) {

        this.revisions = revisions;
    }

    /**
     * The stamp of these revisions, in this order.
     *
     * @param revisions the revisions the reading was taken under; copied, so a later write to the
     *                  array cannot move a stamp a holder has kept
     * @return the stamp
     */
    public static RevisionStamp of(int... revisions) {
        return new RevisionStamp(revisions.clone());
    }

    /**
     * This stamp's revisions followed by another's, for a holder whose reading spans its own
     * revisions and a source's.
     *
     * @param tail the revisions taken after this stamp's; appended last when its length varies
     * @return the joined stamp
     */
    public RevisionStamp append(RevisionStamp tail) {

        var joined = Arrays.copyOf(
            revisions,
            revisions.length + tail.revisions.length);

        System.arraycopy(
            tail.revisions,
            0, // srcPos:
            joined,
            revisions.length,
            tail.revisions.length);

        return new RevisionStamp(joined);
    }

    @Override
    public boolean equals(Object other) {

        return other instanceof RevisionStamp stamp
            && Arrays.equals(revisions, stamp.revisions);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(revisions);
    }

    @Override
    public String toString() {
        return Arrays.toString(revisions);
    }
}

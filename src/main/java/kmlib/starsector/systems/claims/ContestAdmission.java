package kmlib.starsector.systems.claims;

/**
 * How the claim mechanic's own walk met one market: as a competitor it weighed, or as one of the
 * two kinds it carries without ever weighing.
 *
 * <p>The two facts are independent and are kept apart because they are separately true - Galatia
 * Academy is concealed and unregistered at once, a raided pirate base is concealed and listed, and
 * a mod's unregistered colony need not be concealed at all. They are bundled because every reader
 * of a finished contest wants the same one question of them, {@link #isScoredOnItsOwnAccount},
 * and asking it here means no reader has to remember to keep a pair of tests in step.
 *
 * <p>A value rather than two booleans on the breakdown, because they would otherwise sit adjacent
 * in a constructor beside a third: swapping two of them compiles, and since both of these suppress
 * scoring in the same way, most of what reads a contest could not tell the difference. Named and
 * typed, the swap cannot be written.
 *
 * @param isHiddenMarket     whether the market is concealed rather than held in the open. The
 *                           mechanic skips it before scoring, so it takes no standing and can
 *                           neither win nor lose a listing tie - while still counting toward the
 *                           sibling term, which is the one thing it does reach the contest through
 * @param isOffEconomyMarket whether the colony sits outside the economy's own listing - a real
 *                           market on a real entity that was never registered, as vanilla builds
 *                           Galatia Academy. The mechanic walks the economy and nothing else, so
 *                           such a market is never reached at all: unlike a concealed one it does
 *                           not even reach the sibling term
 */
public record ContestAdmission(
    boolean isHiddenMarket,
    boolean isOffEconomyMarket) {

    /** A colony held in the open and listed by the economy - the market the mechanic weighs. */
    public static final ContestAdmission WEIGHED = new ContestAdmission(false, false);

    /**
     * Whether the mechanic weighed the market as a competitor in its own right.
     *
     * <p>Nothing reading a finished contest needs to tell the two exclusions apart - both counted
     * for nothing - so this is the question they collapse into.
     *
     * @return true when the market competed on its own account
     */
    public boolean isScoredOnItsOwnAccount() {
        return !isHiddenMarket && !isOffEconomyMarket;
    }
}

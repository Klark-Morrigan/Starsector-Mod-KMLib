package kmlib.starsector.systems.claims;

/**
 * How the claim mechanic's own walk met one market: as a competitor it weighed, or as one of the
 * two kinds it carries without ever weighing.
 *
 * <p>The two facts are independent and are kept apart because they are separately true - Galatia
 * Academy is concealed and unregistered at once, a raided pirate base is concealed and listed, and
 * a mod's unregistered colony need not be concealed at all. They are bundled because what a reader
 * of a finished contest wants of them is one of two nested questions - whether the market competed
 * ({@link #isScoredOnItsOwnAccount}) or whether it reached the contest at all
 * ({@link #isCountedTowardSiblings}) - and asking either here means no reader has to remember to
 * keep a pair of tests in step.
 *
 * <p>A value rather than two booleans on the breakdown, because they would otherwise sit adjacent
 * in a constructor beside a third: swapping two of them compiles, and since both of these suppress
 * scoring in the same way, most of what reads a contest could not tell the difference. Named and
 * typed, the swap cannot be written.
 *
 * @param isHiddenMarket     whether the market is concealed rather than held in the open. The
 *                           mechanic skips it before scoring, so it takes no standing and can
 *                           neither win nor lose a listing tie - while still counting toward the
 *                           sibling term, which is the one thing it does reach the contest through.
 *                           A flag about the market and not about the place: vanilla also sets it
 *                           on a market it never registered, to keep a station it means the player
 *                           to visit off the economy's books
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
     * A colony held in concealment, which the economy nonetheless lists - a pirate or Path base,
     * as the intels build one. The walk skips it before scoring and it reaches the contest through
     * the sibling term alone.
     */
    public static final ContestAdmission HIDDEN = new ContestAdmission(true, false);

    /**
     * A colony held in the open that the economy does not list - a mod's own unregistered colony,
     * which need conceal nothing. The walk never reaches it at all, so it takes no part in even the
     * sibling term.
     *
     * <p>Vanilla's Galatia Academy is <em>not</em> this shape: it is concealed as well, so it
     * arrives with both facts set and matches none of the three constants here. They name the
     * corners a reader is most often posing, not the whole of what the pair can express.
     */
    public static final ContestAdmission OFF_ECONOMY = new ContestAdmission(false, true);

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

    /**
     * Whether the mechanic counted the market toward its faction's sibling term - the flat point a
     * standing is paid for every other market its faction holds in the same system.
     *
     * <p>The economy's listing is the whole of it. The count walks what the economy lists and nothing
     * else, so a concealed market is counted despite never being scored, and an unregistered one is
     * not counted despite being as present on the map as any other.
     *
     * <p>The broader of the two questions, and the one a reader wants when it is asking whether the
     * market reached the contest <em>at all</em> rather than whether it competed:
     * {@link #isScoredOnItsOwnAccount} is a strictly narrower set.
     *
     * @return true when the market was counted as a sibling
     */
    public boolean isCountedTowardSiblings() {
        return !isOffEconomyMarket;
    }
}

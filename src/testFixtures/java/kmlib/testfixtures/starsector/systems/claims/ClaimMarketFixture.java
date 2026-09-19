package kmlib.testfixtures.starsector.systems.claims;

import kmlib.starsector.entities.EntityNameplate;
import kmlib.starsector.systems.claims.ContestAdmission;
import kmlib.starsector.systems.claims.MarketClaimBreakdown;

import java.util.Locale;
import java.util.OptionalInt;

/**
 * Builds one market's place in a claim contest a fact at a time, for the tests that pose colonies
 * rather than whole standings. Published as a fixture variant so both KMLib's and consuming mods' tests state a
 * market the same way.
 *
 * <p>Named facts rather than the record's own eight-argument constructor, which is what every suite
 * posing markets had otherwise written out for itself. Four of those arguments are an int, an int, a
 * boolean and a boolean, so two of them transposed still compiles and simply poses a colony the case
 * was not about - a size that reads as a sibling count, a market known to the player where the case
 * meant one nobody has discovered.
 *
 * <p>Every fact carries the shape a case is most often not varying, so a case states the one or two
 * it is actually about. What is left unstated is an ordinary colony: held in the open, listed by the
 * economy, discovered, alone in its system and carrying no garrison.
 *
 * <p>Not thread-safe and not reusable across markets: each {@link #startMarket} opens one market,
 * and the setters mutate it until it is built.
 */
public final class ClaimMarketFixture {

    // What a market with no garrison adds on the military term - the absence rather than a nought,
    // which is a market whose garrison was worth nothing and a shape vanilla never builds.
    private static final OptionalInt NO_MILITARY_BONUS = OptionalInt.empty();

    // Where a market falls in the economy's listing when a case is not about the order. The head of
    // it, so a tie posed against such a market is one it won.
    private static final int FIRST_LISTED = 1;

    // How large a colony is when a case is not about the size, and how many others its faction holds
    // beside it when a case is not about the presence term.
    private static final int DEFAULT_MARKET_SIZE = 3;
    private static final int NO_SIBLING_MARKETS = 0;

    private EntityNameplate marketNameplate;
    private String marketId;
    private int listingPosition = FIRST_LISTED;
    private boolean isKnownToPlayer = true;
    private ContestAdmission admission = ContestAdmission.WEIGHED;
    private int marketSize = DEFAULT_MARKET_SIZE;
    private int siblingMarketCount = NO_SIBLING_MARKETS;
    private OptionalInt militaryBonus = NO_MILITARY_BONUS;

    private ClaimMarketFixture(String marketName) {
        this.marketNameplate = EntityNameplate.createUnmarkedNameplate(marketName);
        this.marketId = nameMarketId(marketName);
    }

    /**
     * Opens a market on a colony of the given name, marked with no glyph and identified by an ID
     * derived from that name.
     *
     * <p>The ID follows the name so a case pairing a row with anything else it knows about the same
     * colony has one identity for it throughout, without stating the same string twice.
     *
     * @param marketName how the colony is named to a reader
     * @return the fixture, for the facts a case varies
     */
    public static ClaimMarketFixture startMarket(String marketName) {
        return new ClaimMarketFixture(marketName);
    }

    /**
     * Marks the colony with the glyph the sector map draws it by, in place of the unmarked nameplate
     * the name alone gives it.
     *
     * @param nameplate how the colony is identified to a reader, glyph included
     * @return this fixture
     */
    public ClaimMarketFixture setNameplate(EntityNameplate nameplate) {
        this.marketNameplate = nameplate;
        return this;
    }

    /**
     * States the colony's ID outright, for a case pairing this row with one built elsewhere.
     *
     * @param marketId which colony this is, as {@code MarketAPI#getId} would report it
     * @return this fixture
     */
    public ClaimMarketFixture setMarketId(String marketId) {
        this.marketId = marketId;
        return this;
    }

    /**
     * Puts the colony at the given place in the economy's listing - the order a tied contest is
     * settled in.
     *
     * @param listingPosition where the market falls among the system's owned markets, counting from
     *                        one
     * @return this fixture
     */
    public ClaimMarketFixture setListingPosition(int listingPosition) {
        this.listingPosition = listingPosition;
        return this;
    }

    /**
     * States whether the player knows the colony exists at all.
     *
     * @param isKnownToPlayer false for the colony a display may not name
     * @return this fixture
     */
    public ClaimMarketFixture setKnownToPlayer(boolean isKnownToPlayer) {
        this.isKnownToPlayer = isKnownToPlayer;
        return this;
    }

    /**
     * States how the mechanic's walk met the market, in place of the weighed reading it takes by
     * default.
     *
     * @param admission the admission the contest met the market through
     * @return this fixture
     */
    public ClaimMarketFixture setAdmission(ContestAdmission admission) {
        this.admission = admission;
        return this;
    }

    /**
     * Sizes the colony - the term a claim score starts from.
     *
     * @param marketSize the colony's own size rating
     * @return this fixture
     */
    public ClaimMarketFixture setMarketSize(int marketSize) {
        this.marketSize = marketSize;
        return this;
    }

    /**
     * States how many other markets the same faction holds in the system, each worth a point.
     *
     * @param siblingMarketCount the count, which is also the term
     * @return this fixture
     */
    public ClaimMarketFixture setSiblingMarketCount(int siblingMarketCount) {
        this.siblingMarketCount = siblingMarketCount;
        return this;
    }

    /**
     * Puts a garrison on the colony, worth the given flat bonus.
     *
     * @param militaryBonus what the garrison earns
     * @return this fixture
     */
    public ClaimMarketFixture setMilitaryBonus(int militaryBonus) {
        this.militaryBonus = OptionalInt.of(militaryBonus);
        return this;
    }

    /**
     * The market as the facts stated so far describe it.
     *
     * @return the breakdown
     */
    public MarketClaimBreakdown buildMarket() {
        return new MarketClaimBreakdown(
            marketNameplate,
            marketId,
            listingPosition,
            isKnownToPlayer,
            admission,
            marketSize,
            siblingMarketCount,
            militaryBonus);
    }

    /**
     * The ID the walk that met a colony would have recorded for it, derived from the name so a case
     * naming a market has one identity for it throughout.
     *
     * @param marketName the colony's name
     * @return the ID, lower-cased with its spacing joined
     */
    public static String nameMarketId(String marketName) {
        return marketName.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}

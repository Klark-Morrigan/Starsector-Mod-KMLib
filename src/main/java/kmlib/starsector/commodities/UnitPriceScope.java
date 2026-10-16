package kmlib.starsector.commodities;

/**
 * How far a local price had to reach to find one, nearest first.
 */
public enum UnitPriceScope {

    /** The market standing at the place, priced alone. */
    LOCAL_MARKET,

    /** The average across the markets in the place's location, such as its star system. */
    LOCATION_AVERAGE,

    /** The average across every market in the sector's economy. */
    SECTOR_AVERAGE
}

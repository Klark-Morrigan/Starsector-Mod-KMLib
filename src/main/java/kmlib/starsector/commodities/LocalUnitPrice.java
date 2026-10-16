package kmlib.starsector.commodities;

/**
 * A commodity's unit price at a place, and how far the read had to reach to find it.
 *
 * @param unitPrice  the price of one unit
 * @param priceScope where the price came from
 */
public record LocalUnitPrice(
    float unitPrice,
    UnitPriceScope priceScope) {
}

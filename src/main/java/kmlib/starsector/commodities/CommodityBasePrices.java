package kmlib.starsector.commodities;

import com.fs.starfarer.api.SettingsAPI;

import kmlib.text.KmlibStrings;

/**
 * A commodity's base price as the game's data holds it, for a reader that divides by one. Vanilla ships
 * {@code lobster} with no base price and names {@code luxury_goods} as its demand class, the commodity that carries
 * the price for that class; any mod may also zero a price. So a missing price is read off the demand-class peer
 * before the caller has to decide what to divide by.
 */
public final class CommodityBasePrices {

    /** What {@link #findBasePrice} answers when neither the commodity nor its demand-class peer has a price. */
    public static final float NO_BASE_PRICE = 0f;

    private CommodityBasePrices() {
    }

    /**
     * The commodity's own base price when positive, else its demand-class peer's when positive.
     *
     * @param settings    the settings the commodity specs are read from
     * @param commodityId the commodity
     * @return a positive price, or {@link #NO_BASE_PRICE} when the commodity is unknown or neither carries one
     */
    public static float findBasePrice(SettingsAPI settings, String commodityId) {

        var spec = settings.getCommoditySpec(commodityId);

        if (spec == null) {
            return NO_BASE_PRICE;
        }
        if (spec.getBasePrice() > NO_BASE_PRICE) {
            return spec.getBasePrice();
        }

        var demandClassId = spec.getDemandClass();

        if (!KmlibStrings.hasText(demandClassId) || demandClassId.equals(commodityId)) {
            return NO_BASE_PRICE;
        }

        var peerSpec = settings.getCommoditySpec(demandClassId);

        if (peerSpec == null || peerSpec.getBasePrice() <= NO_BASE_PRICE) {
            return NO_BASE_PRICE;
        }
        return peerSpec.getBasePrice();
    }
}

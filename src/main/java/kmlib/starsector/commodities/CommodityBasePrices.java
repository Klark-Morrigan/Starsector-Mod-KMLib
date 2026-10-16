package kmlib.starsector.commodities;

import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;

import kmlib.text.KmlibStrings;

import java.util.Optional;

/**
 * A commodity's base price as the game's data holds it, for a reader that divides by one. Vanilla ships
 * {@code lobster} with no base price and names {@code luxury_goods} as its demand class, the commodity that carries
 * the price for that class; any mod may also zero a price. So a missing price is read off the demand-class peer
 * before the caller has to decide what to divide by.
 */
public final class CommodityBasePrices {

    private static final float ZERO_PRICE = 0f;

    private CommodityBasePrices() {
    }

    /**
     * The commodity's own base price when positive, else its demand-class peer's when positive.
     *
     * @param settings    the settings the commodity specs are read from
     * @param commodityId the commodity
     * @return a positive price, or empty when the commodity is unknown or neither carries one
     */
    public static Optional<Float> findBasePrice(SettingsAPI settings, String commodityId) {

        var spec = settings.getCommoditySpec(commodityId);
        if (spec == null) {
            return Optional.empty();
        }

        var ownPrice = findPositiveBasePrice(spec);
        if (ownPrice.isPresent()) {
            return ownPrice;
        }

        var demandClassId = spec.getDemandClass();

        if (!KmlibStrings.hasText(demandClassId) || demandClassId.equals(commodityId)) {
            return Optional.empty();
        }

        return Optional
            .ofNullable(settings.getCommoditySpec(demandClassId))
            .flatMap(CommodityBasePrices::findPositiveBasePrice);
    }

    private static Optional<Float> findPositiveBasePrice(CommoditySpecAPI spec) {

        var basePrice = spec.getBasePrice();

        return basePrice > ZERO_PRICE
            ? Optional.of(basePrice)
            : Optional.empty();
    }
}

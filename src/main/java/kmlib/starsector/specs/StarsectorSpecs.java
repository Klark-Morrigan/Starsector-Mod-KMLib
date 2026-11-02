package kmlib.starsector.specs;

import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.Optional;
import java.util.function.Function;

/**
 * Spec lookups that answer an unknown ID with nothing rather than an exception.
 *
 * <p>The game's own lookups disagree on an unknown ID: {@code getHullSpec} and {@code getWeaponSpec} throw a bare
 * {@link RuntimeException}, while {@code getFighterWingSpec} returns {@code null}. An ID a save keeps from a mod since
 * removed reaches them all the same, so each lookup here folds both answers into an empty {@link Optional}.
 */
public final class StarsectorSpecs {

    private StarsectorSpecs() {
    }

    /**
     * Finds the fighter wing spec registered under {@code wingId}.
     *
     * @param settings the settings to look the spec up in
     * @param wingId   the wing spec ID
     * @return the spec, or empty when no loaded mod registers {@code wingId}
     */
    public static Optional<FighterWingSpecAPI> findFighterWingSpec(SettingsAPI settings, String wingId) {

        return findSpec(settings::getFighterWingSpec, wingId);
    }

    /**
     * Finds the ship hull spec registered under {@code hullId}.
     *
     * @param settings the settings to look the spec up in
     * @param hullId   the hull spec ID
     * @return the spec, or empty when no loaded mod registers {@code hullId}
     */
    public static Optional<ShipHullSpecAPI> findHullSpec(SettingsAPI settings, String hullId) {

        return findSpec(settings::getHullSpec, hullId);
    }

    /**
     * Finds the weapon spec registered under {@code weaponId}.
     *
     * @param settings the settings to look the spec up in
     * @param weaponId the weapon spec ID
     * @return the spec, or empty when no loaded mod registers {@code weaponId}
     */
    public static Optional<WeaponSpecAPI> findWeaponSpec(SettingsAPI settings, String weaponId) {

        return findSpec(settings::getWeaponSpec, weaponId);
    }

    // The lookup is a map read behind the throw, so the exception can only mean the ID is unknown. A bound method
    // reference resolves its receiver before the try, so a null settings throws at the caller instead of reading as one.
    private static <T> Optional<T> findSpec(Function<String, T> lookup, String specId) {

        try {
            return Optional.ofNullable(lookup.apply(specId));

        } catch (RuntimeException unknownId) {
            return Optional.empty();
        }
    }
}

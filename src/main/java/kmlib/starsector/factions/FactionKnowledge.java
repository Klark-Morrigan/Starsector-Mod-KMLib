package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.impl.items.BlueprintProviderItem;
import com.fs.starfarer.api.campaign.impl.items.ModSpecItemPlugin;

import java.util.List;
import java.util.function.Predicate;

/**
 * What a faction already knows of what an item teaches: the ships, weapons, fighters and industries a blueprint
 * provides, or the hullmod a modspec carries. A reader handing out loot uses it to hold back items the faction would
 * gain nothing from - RAT Frontiers strips its ruins salvage this way.
 */
public final class FactionKnowledge {

    private FactionKnowledge() {
    }

    /**
     * Whether {@code faction} already knows everything {@code item} teaches.
     *
     * <ul>
     *   <li>A blueprint answers true when the faction knows every ID it provides; one unknown ID answers false. A
     *   blueprint providing nothing has nothing left to teach, so it answers true.</li>
     *   <li>A modspec answers whether the faction knows its hullmod.</li>
     *   <li>Any other item, or none, teaches nothing and answers false, so a caller holding back known items keeps
     *   it.</li>
     * </ul>
     *
     * @param faction the faction whose knowledge decides
     * @param item    the item's plugin, or null for a stack that carries none
     * @return whether the item would teach the faction nothing new
     */
    public static boolean isEverythingTaughtKnown(FactionAPI faction, SpecialItemPlugin item) {

        if (item instanceof BlueprintProviderItem blueprint) {
            return isEveryProvidedIdKnown(faction, blueprint);
        }
        if (item instanceof ModSpecItemPlugin modSpec) {
            return faction.knowsHullMod(modSpec.getModId());
        }
        return false;
    }

    // A null list provides nothing, so it holds no ID the faction could be missing.
    private static boolean areAllKnown(List<String> ids, Predicate<String> isKnown) {

        return ids == null || ids.stream().allMatch(isKnown);
    }

    private static boolean isEveryProvidedIdKnown(FactionAPI faction, BlueprintProviderItem blueprint) {

        return areAllKnown(blueprint.getProvidedShips(), faction::knowsShip)
            && areAllKnown(blueprint.getProvidedWeapons(), faction::knowsWeapon)
            && areAllKnown(blueprint.getProvidedFighters(), faction::knowsFighter)
            && areAllKnown(blueprint.getProvidedIndustries(), faction::knowsIndustry);
    }
}

package kmlib.starsector.rat;

import kmlib.settings.LunaSettingsReader;
import kmlib.starsector.ui.map.presence.CampaignMinimap;

import java.util.function.BooleanSupplier;

/**
 * The {@link CampaignMinimap} answer for Random Assortment of Things: the mod enabled, and its own
 * minimap switched on.
 *
 * <p>Here rather than beside that role because this is the half that knows the mod - its id, its
 * settings field, and the value it ships - while the question the role puts is one any mod
 * replacing the radar could answer.
 *
 * <p>Both halves are needed. The mod ships the minimap off and only replaces the campaign radar
 * once the player turns it on, so "enabled" alone would report a map that is not drawn - and
 * adapting to a minimap that is not there is the fault this role exists to avoid, not a milder
 * version of it.
 *
 * <p>No Random Assortment of Things type is named here, unlike
 * {@link RandomAssortmentOfThingsMatcher} beside it: the minimap switch is a LunaLib field, read
 * by mod id and field id, so an install without the mod resolves nothing of that mod and this
 * needs none of the deferred-reference machinery its own classes would demand. What the presence
 * gate buys here is therefore not safety - the field read alone answers false for a mod that
 * declares no settings - but silence: LunaLib logs an error line for each read of an unknown mod
 * id, so an ungated read asked once a frame would write one per frame, for the whole session, on
 * every install without the mod.
 *
 * <p>Both reads are live rather than settled once, which is the cheaper answer and not a
 * materially worse one. The mod applies its switch at load, so the only window where a live read
 * disagrees with the screen is between the player changing it and the reload that acts on it -
 * and what that costs is an adaptation aimed at a minimap that is not drawn, on a surface that
 * therefore has nothing to answer, rather than anything a player can be shown wrongly.
 */
public final class RandomAssortmentOfThingsMinimap implements CampaignMinimap {

    // The mod's own LunaLib switch for the minimap, and the value it ships. The fallback mirrors
    // that mod's CSV row rather than guessing: an unread field then answers what an untouched
    // install would, which is a radar and no minimap.
    private static final String MINIMAP_ENABLED_FIELD = "rat_enableMinimap";
    private static final boolean DEFAULT_MINIMAP_ENABLED = false;

    private final BooleanSupplier isModEnabled;
    private final BooleanSupplier isMinimapSwitchedOn;

    /** Reads the live mod set and the live LunaLib switch - the pairing outside a test. */
    public RandomAssortmentOfThingsMinimap() {
        this(RandomAssortmentOfThingsPresence::isModEnabled,
            RandomAssortmentOfThingsMinimap::readLiveMinimapSetting);
    }

    /**
     * @param isModEnabled        whether the mod supplying the minimap is enabled
     * @param isMinimapSwitchedOn whether that mod's own minimap switch is on
     */
    RandomAssortmentOfThingsMinimap(
            BooleanSupplier isModEnabled,
            BooleanSupplier isMinimapSwitchedOn) {

        this.isModEnabled = isModEnabled;
        this.isMinimapSwitchedOn = isMinimapSwitchedOn;
    }

    @Override
    public boolean isReplacingRadar() {
        // Short-circuits on the presence gate, so an install without the mod asks LunaLib nothing
        // about settings no mod declares.
        return isModEnabled.getAsBoolean() && isMinimapSwitchedOn.getAsBoolean();
    }

    // Reached only behind the presence gate, so the fallback below stands for a field the mod
    // declares but LunaLib has not loaded a value for, rather than for the mod being absent.
    // LunaLib seeds every field its CSV declares with that row's default at load, so the fallback
    // is reached only by a release of the mod that renames or drops the field - answered as no
    // minimap, which is the same answer an install without the mod gives.
    private static boolean readLiveMinimapSetting() {
        return LunaSettingsReader.getBoolean(
            RandomAssortmentOfThingsPresence.MOD_ID,
            MINIMAP_ENABLED_FIELD,
            DEFAULT_MINIMAP_ENABLED);
    }
}

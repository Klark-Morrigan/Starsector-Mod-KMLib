package kmlib.starsector.intel;

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Common base for intel items that need to land on one or more
 * mod-defined intel tabs. Captures the tab tags at construction and
 * mixes them into every {@link #getIntelTags} call on top of vanilla's
 * "Important" / "New" / "Local" derivation, so subclasses do not need
 * to re-implement the merge boilerplate per intel type.
 *
 * <p>The varargs constructor accepts zero tags, in which case the
 * mix-in is a no-op and the subclass behaves exactly like a vanilla
 * {@link BaseIntelPlugin} - useful for the "expiring but untagged"
 * case so the {@link BaseExpiringIntelPlugin} chain does not force
 * every consumer to declare a tab.</p>
 *
 * <p>Tag order is preserved in iteration order (the backing set is a
 * {@code LinkedHashSet}), so consumers that pass multiple tags get a
 * deterministic merge for any vanilla code that walks the set.</p>
 */
public abstract class BaseTaggedIntelPlugin extends BaseIntelPlugin {

    private final Set<String> extraIntelTags;

    /**
     * Builds an intel item that contributes {@code extraIntelTags} on
     * top of vanilla's tag derivation. Zero tags is legal and yields
     * a pure vanilla pass-through.
     */
    protected BaseTaggedIntelPlugin(String... extraIntelTags) {
        // Defensive copy so a caller cannot mutate the tag set after
        // construction by retaining a reference to their varargs array.
        this.extraIntelTags = new LinkedHashSet<>(Arrays.asList(extraIntelTags));
    }

    /**
     * Vanilla tags merged with the constructor-supplied set. Calls
     * {@code super} first so "Important" / "New" / "Local" still flow
     * through unchanged; the merge runs on every read because the
     * vanilla derivation depends on live state (selection, location).
     */
    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        var tags = super.getIntelTags(map);
        tags.addAll(extraIntelTags);
        return tags;
    }
}

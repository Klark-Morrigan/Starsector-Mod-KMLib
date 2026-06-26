package kmlib.starsector.ui.color;

import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Unified palette extending the set of UI colours available to the KM
 * mod series. Vanilla entries (prefixed {@code VANILLA_}) re-expose the
 * engine's own shades via {@link Misc} suppliers so they keep tracking
 * the live palette - including any player-faction recolours, since
 * {@code VANILLA_PLAYER_BASE} / {@code VANILLA_PLAYER_DARK} resolve from
 * the current player faction (blue by default, but Nex and modded
 * player factions can change it). Custom entries wrap a literal
 * {@link Color} behind the same supplier shape, which keeps the
 * resolution path uniform.
 *
 * <p>Call {@link #resolve()} to obtain the live {@link Color}. The
 * resolver null-checks the supplier output and tags the failure with the
 * enum name, because {@code Misc} accessors can return {@code null}
 * during early engine boot and letting that propagate into UI code
 * produces a much less actionable error.
 */
public enum StarsectorUiColor {
    VANILLA_GRAY(Misc::getGrayColor),
    VANILLA_TEXT(Misc::getTextColor),
    VANILLA_PLAYER_BASE(Misc::getBasePlayerColor),
    VANILLA_PLAYER_DARK(Misc::getDarkPlayerColor),
    VANILLA_HIGHLIGHT_GOLD(Misc::getHighlightColor),
    VANILLA_HIGHLIGHT_RED(Misc::getNegativeHighlightColor),
    VANILLA_HIGHLIGHT_GREEN(Misc::getPositiveHighlightColor),
    WHITE(Color.WHITE),
    DIM_GRAY(new Color(130, 130, 130)),
    ORANGE(new Color(255, 100, 0, 255)),
    DARK_RED(new Color(70, 20, 20)),
    MUTED_RED(new Color(150, 50, 45)),
    BRIGHT_RED(new Color(255, 90, 80)),
    DARK_GREEN(new Color(35, 80, 45)),
    BRIGHT_GREEN(new Color(90, 220, 95)),
    /**
     * Literal copy of the vanilla {@code player} faction's
     * {@code baseUIColor} from
     * {@code starsector-core/data/world/factions/player.faction}. Use this
     * when a callsite needs the iconic Starsector light blue regardless of
     * which faction the current player has chosen (Nex/modded player
     * factions change {@link #VANILLA_PLAYER_BASE}).
     */
    LIGHT_BLUE(new Color(170, 222, 255)),
    /**
     * Literal copy of the vanilla {@code player} faction's
     * {@code darkUIColor} from
     * {@code starsector-core/data/world/factions/player.faction}. Pairs
     * with {@link #LIGHT_BLUE} as the frozen baseline player palette - use
     * it when the dark companion shade must stay constant regardless of
     * the active player faction (which is what
     * {@link #VANILLA_PLAYER_DARK} tracks).
     */
    DARK_BLUE(new Color(31, 94, 112, 175));

    private final Supplier<Color> source;

    StarsectorUiColor(Supplier<Color> source) {
        this.source = source;
    }

    StarsectorUiColor(Color literal) {
        // Wrapping the literal in a supplier keeps resolve() uniform: no
        // branch on vanilla-vs-custom at call time, just one code path
        // that always null-checks the result.
        this(() -> literal);
    }

    public Color resolve() {
        var resolved = source.get();
        return Objects.requireNonNull(resolved, () -> "Missing color value for " + name());
    }
}

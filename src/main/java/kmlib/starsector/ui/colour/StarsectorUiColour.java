package kmlib.starsector.ui.colour;

import com.fs.starfarer.api.Global;
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
 * <p>Entries are grouped by where their value comes from, and the two
 * groups answer differently to a restyled install: a live read follows
 * it, a frozen literal does not. Which behaviour a callsite wants is the
 * whole of the choice between them, so the bands are kept apart rather
 * than interleaved by colour name.
 *
 * <p>A third kind is deliberately absent: a shade worked out from a live
 * read by some renderer's own rule - a fill composited over its backdrop,
 * a glow added onto it - is not an entry here. Its rule is a fact about
 * how one thing is painted rather than about the palette, and an entry
 * here would carry that paint's constants into a catalogue every other
 * widget reads. Such a shade belongs beside the paint that defines it,
 * built from the entries below.
 *
 * <p>Call {@link #resolve()} to obtain the live {@link Color}. The
 * resolver null-checks the supplier output and tags the failure with the
 * enum name, because {@code Misc} accessors can return {@code null}
 * during early engine boot and letting that propagate into UI code
 * produces a much less actionable error.
 */
public enum StarsectorUiColour {

    // -- Live engine reads ----------------------------------------------------------------------------
    // Whatever the running install says the colour is, asked afresh every time. A settings restyle or a
    // player-faction recolour reaches a consumer of these without it doing anything.

    VANILLA_GRAY(Misc::getGrayColor),
    VANILLA_TEXT(Misc::getTextColor),
    VANILLA_BUTTON_TEXT(Misc::getButtonTextColor),
    // The engine's dark button fill from settings.json ("buttonBgDark") - the dark teal a resting
    // button/tab fills with, and the shade the engine's own map tabs are painted from. The fixed UI
    // palette, NOT the player-faction shades: a vanilla tab takes no faction colour.
    VANILLA_BUTTON_BG_DARK(() -> Global.getSettings().getColor("buttonBgDark")),
    VANILLA_PLAYER_BASE(Misc::getBasePlayerColor),
    VANILLA_PLAYER_BRIGHT(Misc::getBrightPlayerColor),
    VANILLA_PLAYER_DARK(Misc::getDarkPlayerColor),
    VANILLA_HIGHLIGHT_GOLD(Misc::getHighlightColor),
    VANILLA_HIGHLIGHT_RED(Misc::getNegativeHighlightColor),
    VANILLA_HIGHLIGHT_GREEN(Misc::getPositiveHighlightColor),

    // -- Frozen literals ------------------------------------------------------------------------------
    // Shades that stay put whatever the install does, either because they are our own or because a
    // consumer needs the vanilla baseline to hold still while the live palette moves around it.

    BLACK(Color.BLACK),
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
    DARK_BLUE(new Color(31, 94, 112, 175)),
    /**
     * The shade the vanilla SELECTED map tab reads as, sampled in-game
     * ({@code #487b8d}), and {@link #DARK_TEAL} its resting companion
     * ({@code #15404d}). Kept as the measured record of what those tabs
     * looked like on one install at one moment.
     *
     * <p>Nothing paints from these: a tab strip works its fills out from
     * the engine's own colours by the engine's own rule, so it follows a
     * restyled install instead of holding a sample of an unstyled one. A
     * callsite wanting the shade of that particular pair - to match it
     * deliberately rather than to track it - can still name them.
     */
    STEEL_BLUE(new Color(72, 123, 141)),
    /** The resting companion to {@link #STEEL_BLUE}; see there. */
    DARK_TEAL(new Color(21, 64, 77));

    private final Supplier<Color> source;

    StarsectorUiColour(Supplier<Color> source) {
        this.source = source;
    }

    StarsectorUiColour(Color literal) {
        // Wrapping the literal in a supplier keeps resolve() uniform: no
        // branch on vanilla-vs-custom at call time, just one code path
        // that always null-checks the result.
        this(() -> literal);
    }

    public Color resolve() {
        var resolved = source.get();
        return Objects.requireNonNull(
            resolved,
            () -> "Missing colour value for " + name());
    }
}

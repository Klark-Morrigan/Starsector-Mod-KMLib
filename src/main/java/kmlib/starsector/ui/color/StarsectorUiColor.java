package kmlib.starsector.ui.color;

import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Palette of UI colours shared across the KM mod series. Vanilla
 * shades route through {@link Misc} suppliers so they track the engine's
 * palette automatically; custom shades hold a literal {@link Color} so
 * pure-data consumers can resolve them without booting Starsector.
 *
 * <p>The accessor methods {@link #starsectorColor()},
 * {@link #customColor()}, and {@link #isCustom()} are package-private on
 * purpose: only {@link StarsectorUiColorProvider} and the colocated test
 * need to inspect which source backs an entry, and exposing that surface
 * publicly would invite callers to special-case the resolver instead of
 * going through it.
 */
public enum StarsectorUiColor {
    GRAY(Misc::getGrayColor),
    TEXT_WHITE(Misc::getTextColor),
    BLUE(Misc::getBasePlayerColor),
    DARK_BLUE(Misc::getDarkPlayerColor),
    GOLD(Misc::getHighlightColor),
    RED(Misc::getNegativeHighlightColor),
    GREEN(Misc::getPositiveHighlightColor),
    WHITE(Color.WHITE),
    DIM_GRAY(new Color(130, 130, 130)),
    ORANGE(new Color(255, 100, 0, 255)),
    DARK_RED(new Color(70, 20, 20)),
    MUTED_RED(new Color(150, 50, 45)),
    BRIGHT_RED(new Color(255, 90, 80)),
    DARK_GREEN(new Color(35, 80, 45)),
    BRIGHT_GREEN(new Color(90, 220, 95)),
    LIGHT_BLUE(new Color(100, 180, 255));

    private final Supplier<Color> starsectorColor;
    private final Color customColor;

    StarsectorUiColor(Supplier<Color> starsectorColor) {
        this(starsectorColor, null);
    }

    StarsectorUiColor(Color customColor) {
        this(null, customColor);
    }

    StarsectorUiColor(Supplier<Color> starsectorColor, Color customColor) {
        if (starsectorColor == null && customColor == null) {
            throw new IllegalArgumentException("A color must define a Starsector source or a custom value.");
        }
        this.starsectorColor = starsectorColor;
        this.customColor = customColor;
    }

    Optional<Supplier<Color>> starsectorColor() {
        return Optional.ofNullable(starsectorColor);
    }

    Optional<Color> customColor() {
        return Optional.ofNullable(customColor);
    }

    boolean isCustom() {
        return customColor != null;
    }
}

package kmlib.starsector.ui.color;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Resolves a {@link StarsectorUiColor} entry to its {@link Color}.
 * Vanilla shades invoke the entry's supplier (so the live
 * {@link com.fs.starfarer.api.util.Misc} palette is honoured); custom
 * shades return the literal stored on the enum. Lives in the same
 * package as {@link StarsectorUiColor} so it can read the
 * package-private source accessors without exposing them to the wider
 * API.
 */
public final class StarsectorUiColorProvider {
    private StarsectorUiColorProvider() {
    }

    public static Color get(StarsectorUiColor rawColor) {
        Objects.requireNonNull(rawColor, "rawColor");

        Optional<Supplier<Color>> starsectorSource = rawColor.starsectorColor();
        if (starsectorSource.isPresent()) {
            return resolveStarsectorColor(rawColor, starsectorSource.get());
        }

        return rawColor.customColor()
            .orElseThrow(() -> new IllegalStateException("Missing color value for " + rawColor.name()));
    }

    private static Color resolveStarsectorColor(
            StarsectorUiColor rawColor,
            Supplier<Color> starsectorSource) {
        Color resolvedColor = starsectorSource.get();
        return Objects.requireNonNull(resolvedColor, "Starsector color for " + rawColor.name());
    }
}

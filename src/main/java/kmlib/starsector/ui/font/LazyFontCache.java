package kmlib.starsector.ui.font;

import com.fs.starfarer.api.Global;

import org.apache.log4j.Logger;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads LazyLib bitmap fonts and caches them, so a face loads once however many times it
 * is asked for and a face that will not load is logged once and then skipped rather than
 * retried - and re-logged - on every call.
 *
 * <p>The load-once / fail-once-logged policy that every caller drawing cached text needs,
 * kept in one place so no mod re-implements it around LazyLib's own
 * {@link LazyFont#loadFont}.
 *
 * <p>Guards a missing or malformed {@code .fnt}. A missing LazyLib is a different failure -
 * {@code NoClassDefFoundError}, raised where no check can reach - and is prevented by the
 * {@code mod_info.json} dependency instead.
 */
public final class LazyFontCache {
    private static final Logger LOG = Global.getLogger(LazyFontCache.class);

    // Faces loaded so far, cached by path so a face already selected loads once even
    // after switching away and back. failedPaths remembers a face that would not load, so
    // its FontException is logged once, not on every call.
    private static final Map<String, LazyFont> FONT_BY_PATH = new HashMap<>();
    private static final Set<String> FAILED_FONT_PATHS = new HashSet<>();

    // Caches only; never instantiated.
    private LazyFontCache() {
    }

    /**
     * The loaded, cached face for one of the game's atlases, or null when that face cannot
     * load (a missing or malformed {@code .fnt}). A face known to have failed returns null
     * without retrying.
     *
     * @param font the atlas to load; the enum names its own loadable path
     * @return the cached face, or null when it will not load
     */
    public static LazyFont loadByFace(StarsectorFont font) {
        return getFont(font.resolvePath());
    }

    // Loads one face and caches it by path. A face known to have failed returns null
    // without retrying; a FontException (a missing or malformed .fnt) is logged once for
    // that path so a caller in a per-frame rebuild loop does not flood the log or throw.
    private static LazyFont getFont(String fontPath) {
        var cached = FONT_BY_PATH.get(fontPath);
        if (cached != null) {
            return cached;
        }
        if (FAILED_FONT_PATHS.contains(fontPath)) {
            return null;
        }
        try {
            var loaded = LazyFont.loadFont(fontPath);
            FONT_BY_PATH.put(fontPath, loaded);
            return loaded;
        } catch (FontException exception) {
            FAILED_FONT_PATHS.add(fontPath);
            LOG.error(
                    "Could not load font '"
                            + fontPath
                            + "'; text using this font disabled this session",
                    exception);
            return null;
        }
    }
}

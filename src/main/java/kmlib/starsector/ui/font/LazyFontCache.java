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
 * Loads LazyLib bitmap fonts by their {@code graphics/fonts} basename and caches them,
 * so a face loads once however many times it is asked for and a face that will not load
 * is logged once and then skipped rather than retried - and re-logged - on every call.
 *
 * <p>The load-once / fail-once-logged policy that every caller drawing cached text needs,
 * kept in one place so no mod re-implements it around LazyLib's own
 * {@link LazyFont#loadFont}. LazyLib is a soft dependency of KMLib (compileOnly, no
 * {@code mod_info.json} entry): this class classloads only when a caller invokes it, so a
 * mod that never draws cached text and never ships LazyLib never resolves it.
 */
public final class LazyFontCache {
    private static final Logger LOG = Global.getLogger(LazyFontCache.class);

    // The game's bitmap fonts live under graphics/fonts with a .fnt extension, so a
    // basename resolves to a loadable path by wrapping.
    private static final String FONT_DIR = "graphics/fonts/";
    private static final String FONT_EXTENSION = ".fnt";

    // Faces loaded so far, cached by path so a face already selected loads once even
    // after switching away and back. failedPaths remembers a face that would not load, so
    // its FontException is logged once, not on every call.
    private static final Map<String, LazyFont> FONT_BY_PATH = new HashMap<>();
    private static final Set<String> FAILED_FONT_PATHS = new HashSet<>();

    // Caches only; never instantiated.
    private LazyFontCache() {
    }

    /**
     * The face for a {@code graphics/fonts} basename (e.g. {@code insignia15LTaa}),
     * loaded and cached, or null when that face cannot load (a missing or malformed
     * {@code .fnt}). A face known to have failed returns null without retrying.
     *
     * @param basename the font's basename under {@code graphics/fonts}, without the
     *                 {@code .fnt} suffix
     * @return the cached face, or null when it will not load
     */
    public static LazyFont loadByBasename(String basename) {
        return getFont(FONT_DIR + basename + FONT_EXTENSION);
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
            LOG.error("Could not load font '" + fontPath
                    + "'; text using this font disabled this session", exception);
            return null;
        }
    }
}

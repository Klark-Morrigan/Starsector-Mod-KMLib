package kmlib.starsector.ui.font;

/**
 * The face the game's {@code defaultFont} setting names, where that is a file {@link StarsectorFont} does
 * not name. Held by its path rather than mapped onto a face KM knows, because the file is whoever set the
 * setting's choice - a language pack pointing it at its own atlas is the likeliest - and the point of
 * reaching it is that it may hold glyphs no face KM knows does.
 *
 * <p>Its smoothing is read off its descriptor rather than stated, KM having no table of a file it did not
 * ship: {@link AtlasSmoothing#resolveFromInfoLine} is the same rule the enum's own values were taken by.
 *
 * @param path       the loadable {@code .fnt} path the setting names
 * @param smoothing  whether the atlas's glyphs want interpolating, as its descriptor states
 */
public record DeclaredFontAtlas(
    String path,
    AtlasSmoothing smoothing) implements FontAtlas {

    @Override
    public String resolvePath() {
        return path;
    }

    @Override
    public AtlasSmoothing getSmoothing() {
        return smoothing;
    }
}

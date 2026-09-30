package kmlib.starsector.ui.font.installed;

import kmlib.starsector.ui.font.FaceResolver;
import kmlib.starsector.ui.font.FontAtlas;
import kmlib.starsector.ui.font.SettledFaceMemo;
import kmlib.starsector.ui.font.TextFace;

import java.util.List;
import java.util.function.Function;

/**
 * Binds the font types to the running game's install: a face at the size its installed atlas states, a
 * resolver over the installed atlases, and a memo settling faces through it. The one place the live
 * readers are handed to them, so no two styles read an install two ways.
 */
public final class InstalledFaces {

    // Composes only; never instantiated.
    private InstalledFaces() {
    }

    /**
     * Builds {@code atlas} at its native size as the running game's install states it, read through LazyLib.
     *
     * @param atlas the atlas the text draws in
     * @return the face at its installed native size
     */
    public static TextFace createNativeFace(FontAtlas atlas) {
        return TextFace.createNativeFace(atlas, LazyFontLineHeightReader::readLineHeight);
    }

    /**
     * A memo settling each text's face against the running game's installed atlases, the resolver built
     * on first settling rather than here.
     *
     * @param probeTextReader reads one of the caller's kinds of text, once per kind
     * @param <K>             the caller's kinds of text
     * @return the memo, settling nothing until asked
     */
    public static <K> SettledFaceMemo<K> createFaceMemo(Function<K, List<String>> probeTextReader) {
        return new SettledFaceMemo<>(InstalledFaces::createFaceResolver, probeTextReader);
    }

    /**
     * @return a resolver reading the running game's installed atlases through LazyLib, trying the face the
     *         game's settings declare as its default before the last resort
     */
    public static FaceResolver createFaceResolver() {

        return new FaceResolver(
            LazyFontLineHeightReader::readLineHeight,
            LazyFontGlyphCoverageReader::coversText,
            GameDefaultFontReader.readDefaultFont());
    }
}

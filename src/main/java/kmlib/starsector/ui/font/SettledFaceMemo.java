package kmlib.starsector.ui.font;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The face each text settles on, held once settled: the face the text asks for where the installed atlas
 * holds everything it may say, and otherwise the first face down its fallback walk that does.
 *
 * <p>A text names the face it asks for and the kinds of text it is made of - its probes: a caller's own
 * kinds, such as faction names or its mod's strings, which it reads once each through the reader it hands
 * in. Held
 * rather than resolved per draw, because settling reads every glyph of every text of those kinds; two
 * texts asking for one face against the same kinds share one answer, which keeps a line measured in one
 * face from being drawn in another. A face that moves off the one asked for is logged once, which is the
 * first thing a report of text drawn wrongly is read against.
 *
 * <p>Asked on every draw, so a settled answer is found without allocating: the kinds are looked up as the
 * set the caller holds, which a caller keeps as a constant.
 *
 * @param <K> the caller's kinds of text
 */
public final class SettledFaceMemo<K> {

    private static final Logger LOG = Logger.getLogger(SettledFaceMemo.class);

    private final Map<StarsectorFont, Map<Set<K>, FontAtlas>> faceByProbesByFont = new EnumMap<>(StarsectorFont.class);
    private final Map<K, List<String>> textsByProbe = new HashMap<>();

    // Both null only on the unsettled memo, which settles nothing; every other memo is handed both.
    private final Supplier<FaceResolver> faceResolverSource;
    private final Function<K, List<String>> probeTextReader;

    private FaceResolver faceResolver;

    /**
     * @param faceResolverSource builds the resolver faces are settled through, on first settling
     * @param probeTextReader    reads one kind of text, once per kind; a kind read as null holds no text
     */
    public SettledFaceMemo(Supplier<FaceResolver> faceResolverSource, Function<K, List<String>> probeTextReader) {

        this.faceResolverSource = Objects.requireNonNull(faceResolverSource, "faceResolverSource");
        this.probeTextReader = Objects.requireNonNull(probeTextReader, "probeTextReader");
    }

    // The unsettled memo's: no resolver and no reader, so nothing can be settled or read.
    private SettledFaceMemo() {
        this.faceResolverSource = null;
        this.probeTextReader = null;
    }

    /**
     * For a caller with no text to hold a face to - no game loaded, say - where every text keeps the face
     * it asks for.
     *
     * @param <K> the caller's kinds of text
     * @return a memo answering each text with the face it asks for, reading nothing
     */
    public static <K> SettledFaceMemo<K> createUnsettled() {
        return new SettledFaceMemo<>();
    }

    /**
     * The face a text draws in.
     *
     * @param requestedFont the face the text asks for
     * @param probes        the kinds of text it is made of
     * @return the face the text settles on
     */
    public FontAtlas settleFace(StarsectorFont requestedFont, Set<K> probes) {

        // Only the unsettled memo holds no resolver.
        if (faceResolverSource == null) {
            return requestedFont;
        }

        var faceByProbes = faceByProbesByFont.get(requestedFont);
        var settledFace = faceByProbes == null
            ? null
            : faceByProbes.get(probes);

        if (settledFace != null) {
            return settledFace;
        }
        settledFace = resolveFace(requestedFont, probes);

        faceByProbesByFont
            .computeIfAbsent(requestedFont, font -> new HashMap<>())
            .put(Set.copyOf(probes), settledFace);

        return settledFace;
    }

    /** Forgets every settled face and every text read, for a holder being released. */
    public void discardFaces() {
        faceByProbesByFont.clear();
        textsByProbe.clear();
    }

    private FontAtlas resolveFace(StarsectorFont requestedFont, Set<K> probes) {

        if (faceResolver == null) {
            faceResolver = faceResolverSource.get();
        }

        var texts = new ArrayList<String>();

        for (var probe : probes) {
            texts.addAll(textsByProbe.computeIfAbsent(probe, this::readProbeTexts));
        }

        var settledFace = faceResolver.resolveFont(requestedFont, texts);
        if (!settledFace.equals(requestedFont)) {

            LOG.info("Text asking for " + requestedFont.getBasename()
                + " against " + probes
                + " draws in " + settledFace.resolvePath());
        }
        return settledFace;
    }

    // A reader with nothing to say for a kind may answer null; that kind then holds the face to nothing.
    private List<String> readProbeTexts(K probe) {
        return Objects.requireNonNullElse(probeTextReader.apply(probe), List.of());
    }
}

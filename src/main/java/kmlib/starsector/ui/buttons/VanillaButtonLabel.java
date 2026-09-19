package kmlib.starsector.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.LabelAPI;

import kmlib.logging.SessionWarning;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.coreui.CoreUiMethod;
import kmlib.starsector.ui.coreui.CoreUiMethods;
import kmlib.text.KmlibStrings;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

/**
 * The words on a button the game built, and how a key bound to that button is said in them.
 *
 * <p>Needed because the published button interface does not reach these words. Its text accessors
 * answer for one of the several kinds of button the game builds and quietly do nothing for the
 * rest - they read back null and write nowhere, through the interface that looks like it should
 * work. The same split is why the game's own key announcement never reaches those other kinds: the
 * call that takes a bare keycode writes the key into the words for that one kind and leaves every
 * other bound to a key it never mentions.
 *
 * <p>What the words <em>are</em> reachable through is the published label the engine draws them
 * with, which every kind of button holds somewhere beneath it. So this finds that label rather than
 * naming the widget holding it, and everything said afterwards is said through the modding interface
 * like any other label.
 *
 * <p>The search never matches an obfuscated name. It takes the two accessors the game leaves alone,
 * and follows the hop between them by calling it and looking at what came back rather than by
 * trusting what it promises to return - the game declares that hop as something broader than the
 * piece it hands over, and tests the answer itself.
 *
 * <p>Not finding the words costs whatever was going to be said and nothing else, which is what makes
 * this safe to reach for while decorating a widget somebody else drew.
 */
public final class VanillaButtonLabel {

    private static final Logger LOG = Global.getLogger(VanillaButtonLabel.class);

    // Says once per session that a button's words could not be reached, rather than on every attempt
    // - a caller redecorating a rebuilt widget asks again on every open of the screen carrying it.
    private static final SessionWarning WARNING = new SessionWarning(LOG);

    // The two hops the game itself takes to a button's words, both of them names the game leaves
    // unobfuscated because they are part of what a widget publishes.
    private static final String RENDERER_ACCESSOR = "getRenderer";
    private static final String TITLE_ACCESSOR = "getTitle";

    // Every object's own shape accessor, which leads out of the widget tree rather than down it.
    private static final String CLASS_ACCESSOR = "getClass";

    // Values from the standard library are the end of the walk: a widget's words are held by another
    // widget, so a string, a list or a colour is somewhere the search has already gone wrong.
    private static final String STANDARD_LIBRARY_PREFIX = "java.";

    // How far below a button its words sit: its renderer, and the piece that renderer holds. Stated
    // rather than left open because an unbounded walk over a live widget tree would call its way
    // across half the screen looking for something that is two hops away or nowhere.
    private static final int MAX_HOPS_BELOW_RENDERER = 2;

    // How the game's own buttons wear a key their words do not already contain - "Starscape [1]".
    private static final String SHORTCUT_SUFFIX = " [%s]";

    private final LabelAPI label;

    private VanillaButtonLabel(LabelAPI label) {
        this.label = label;
    }

    /**
     * Finds the words on a button the game built.
     *
     * <p>The caller says what the button was built to read, and only a label already saying exactly
     * that is accepted. A widget holds more than one label - a piece of chrome, a tooltip's own, an
     * empty one waiting to be filled - so a search taking the first it met would come back holding
     * something, write into it, and change nothing anyone can see. Matching on the words turns that
     * silent wrong answer into no answer, which is one that gets logged.
     *
     * @param button        the widget to look under, which is the game's own rather than anything
     *                      drawn by the caller
     * @param expectedWords what the button was built reading, which is how its own label is told
     *                      from every other label beneath it
     * @return its words as something that can be read and written, or null where they cannot be
     *         reached - logged once, and never an error, the widget being somebody else's
     */
    public static VanillaButtonLabel resolveLabelOf(Object button, String expectedWords) {

        if (!KmlibStrings.hasText(expectedWords)) {
            return null;
        }

        try {
            var renderer = readNoArg(button, RENDERER_ACCESSOR);
            var label = findLabelUnder(
                renderer == null ? button : renderer,
                LabelSearch.forWords(expectedWords));

            if (label == null) {
                WARNING.warnOnce(
                    "No label reading \"" + expectedWords + "\" was found under the button the game "
                        + "built for it, so nothing can be said in its words.");
            }
            return label == null ? null : new VanillaButtonLabel(label);

        } catch (Throwable failure) {

            // Throwable rather than Exception: reading a member resolves every type in its
            // signature, so a shape that has moved arrives as an Error, and the bypass the reads go
            // through lets a checked throw escape unannounced.
            WARNING.warnOnce(
                "A button the game built could not be read for its words, so nothing can be said in "
                    + "them.",
                failure);
            return null;
        }
    }

    /**
     * Says which key presses the button, in the words the button already wears.
     *
     * <p>The game's own rule, applied to a button the game will not apply it to: a key whose name
     * already occurs in the words has that occurrence lit rather than repeated, and one that does
     * not is spelled out after them in a bracket. That is why a map filter reads "Starscape [1]"
     * while a core tab reads "Chara(c)ter" - one rule, two outcomes, and following it is what keeps
     * a decorated button from being the one thing on its row that announces itself differently.
     *
     * @param keyName what the key is called, as the key table names it
     */
    public void announceShortcut(String keyName) {

        var words = label.getText();

        if (!KmlibStrings.hasText(words) || !KmlibStrings.hasText(keyName)) {
            return;
        }

        var occurrence = words.toLowerCase(Locale.ROOT).indexOf(keyName.toLowerCase(Locale.ROOT));

        if (occurrence >= 0) {
            // Cut from the words rather than reused from the key's own name, so the run handed over
            // is a substring of what is drawn: the match is made without regard to case and the
            // highlight is not, so lighting "M" where the words hold "m" would light nothing.
            lightRun(words.substring(occurrence, occurrence + keyName.length()));
            return;
        }

        var bracketed = SHORTCUT_SUFFIX.formatted(keyName);

        label.setText(words + bracketed);
        lightRun(bracketed.trim());
    }

    // Whether a member is worth following in search of the words: something the node holds, asked
    // for without arguments, and not a value out of the standard library - a widget's words are held
    // by another widget, so a hop into a string or a list is a hop out of the tree.
    private static boolean isFollowableHop(CoreUiMethod method) {

        var held = method.getReturnType();

        return method.getParameterTypes().isEmpty()
            && !held.isPrimitive()
            && !held.getName().startsWith(STANDARD_LIBRARY_PREFIX)
            && !CLASS_ACCESSOR.equals(method.getName());
    }

    // The words under a node: its own title, or the title of something it holds.
    //
    // The hop between a button's renderer and the piece carrying its words is followed by calling it
    // and looking at what came back, rather than by reading what it promises to return. It has to
    // be: the game declares that hop as something broader than the piece it actually hands over and
    // tests the answer itself, so a search that trusted the declared type would skip the one member
    // that leads anywhere.
    //
    // Bounded to the depth the game uses, and every call guarded: a widget asked a question it does
    // not care for answers by throwing, and that is a dead end here rather than a failure.
    private static LabelAPI findLabelUnder(Object node, LabelSearch search) {

        if (node == null || !search.isFirstVisitTo(node)) {
            return null;
        }

        var here = search.acceptIfReading(node);
        if (here != null) {
            return here;
        }

        var title = search.acceptIfReading(readNoArg(node, TITLE_ACCESSOR));
        if (title != null) {
            return title;
        }

        return search.hasHopsLeft() ? findLabelBelow(node, search) : null;
    }

    // One level down, through everything the node holds, first match winning.
    private static LabelAPI findLabelBelow(Object node, LabelSearch search) {

        for (var method : CoreUiMethods.readPublicMethodsOf(node.getClass())) {

            if (!isFollowableHop(method)) {
                continue;
            }

            var label = findLabelUnder(readQuietly(method, node), search.oneHopDeeper());

            if (label != null) {
                return label;
            }
        }
        return null;
    }

    // What a member answers, or nothing where asking it was the wrong question. Throwable rather
    // than Exception for the reason the resolve above catches one: a member of a shape that has
    // moved fails as an Error, and the reach these go through lets a checked throw escape unnoticed.
    private static Object readQuietly(CoreUiMethod method, Object node) {

        try {
            return method.invokeOn(node);
        } catch (Throwable failure) {
            return null;
        }
    }

    // Calls a published no-arg accessor by name, answering nothing where the shape does not offer
    // one. By name because these two names are the game's own contract rather than regenerated
    // members, and the alternative - matching "a method answering a renderer" by shape - would match
    // whatever else a widget happens to hand back.
    private static Object readNoArg(Object instance, String accessor) {

        if (instance == null) {
            return null;
        }

        return CoreUiMethods.readPublicMethodsOf(instance.getClass())
            .stream()
            .filter(method -> accessor.equals(method.getName()))
            .filter(method -> method.getParameterTypes().isEmpty())
            .findFirst()
            .map(method -> method.invokeOn(instance))
            .orElse(null);
    }

    // Lights one run of the words in the colour the game lights a key in.
    //
    // Through the label's first-occurrence highlight and its single highlight colour, and not
    // through the run list and colour list a paragraph is tinted with. The label keeps those as two
    // separate mechanisms - a range of indices, and a per-character mask - and each clears the other
    // when set. A button's own key is drawn by the range, so a mask set on its label is honoured by
    // nothing that draws the button: the tint lands and never shows. The range is also what the game
    // sets for its own keyed buttons, colour and all, so this is the same write in the same place.
    //
    // Both calls or neither: a range without a colour draws in whatever the last caller left.
    private void lightRun(String run) {

        label.highlightFirst(run);

        // The shade the engine gives a key on a button rather than the one it gives an emphasised
        // word. They resolve alike today, being two names for one settings key, but they are two
        // roles - so a restyle that parts them should part this from prose as well.
        label.setHighlightColor(StarsectorUiColour.VANILLA_BUTTON_SHORTCUT.resolve());
    }

    /**
     * One walk in progress: what is being looked for, how much further it may go, and where it has
     * already been.
     *
     * <p>Together rather than as three arguments threaded down the recursion, because they travel
     * together and only one of them changes on the way: a walk that dropped the visited set on one
     * branch, or carried a depth belonging to another, is a walk that loops or overruns, and neither
     * is visible at a call site handing over four separate things.
     *
     * @param expectedWords what the button was built reading, which is the only thing that tells its
     *                      own label from every other label beneath it
     * @param hopsLeft      how many levels further down the search may go
     * @param visited       the nodes already seen, shared by every branch so a widget tree that
     *                      leads back to itself is walked once rather than forever
     */
    private record LabelSearch(String expectedWords, int hopsLeft, Set<Object> visited) {

        static LabelSearch forWords(String expectedWords) {

            // Visited by identity: two widgets are the same node here only if they are the same
            // object, and asking a live widget whether it equals another is a question it may answer
            // expensively or not at all.
            return new LabelSearch(
                expectedWords,
                MAX_HOPS_BELOW_RENDERER,
                Collections.newSetFromMap(new IdentityHashMap<>()));
        }

        // A candidate, but only where it is drawable words already reading what the button was built
        // with. Anything else is somebody else's label that happens to be within reach.
        LabelAPI acceptIfReading(Object candidate) {

            return candidate instanceof LabelAPI label && expectedWords.equals(label.getText())
                ? label
                : null;
        }

        boolean hasHopsLeft() {
            return hopsLeft > 0;
        }

        // The same search one level down. The visited set is shared rather than copied, so a node
        // reached by two branches is walked by the first of them alone.
        LabelSearch oneHopDeeper() {
            return new LabelSearch(expectedWords, hopsLeft - 1, visited);
        }

        boolean isFirstVisitTo(Object node) {
            return visited.add(node);
        }
    }
}

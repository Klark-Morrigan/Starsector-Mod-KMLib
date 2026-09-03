package kmlib.starsector.ui.map.controls;

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
 * <p>Needed because the published button interface does not reach these words. Its own text
 * accessors answer only for one of the several kinds of button the game builds, and the kind a map's
 * filter row is furnished with is not that one - so a control appended there reads back nothing and
 * writes back nothing, silently, through the interface that looks like it should work. The same
 * split is why the game's own key announcement never reaches such a button: the call that takes a
 * bare keycode writes the key into the words for that one button kind and leaves every other kind
 * bound to a key it never mentions.
 *
 * <p>What the words <em>are</em> reachable through is the published label the engine draws them
 * with, which every kind of button holds somewhere beneath it. So this finds that label rather than
 * naming the widget that holds it, and everything said afterwards is said through the modding
 * interface like any other label.
 *
 * <p>The search is by type and by published accessor, never by an obfuscated name. It follows the
 * button's renderer, and then the one hop below it that leads to a title - and it takes that hop
 * only where the return type <em>already declares</em> a title accessor, so nothing is called
 * speculatively on a live widget to find out what it answers.
 *
 * <p>Not finding one costs the announcement and nothing else. The key is bound before any of this
 * is attempted, so a button whose words cannot be reached still answers its key; what the player
 * loses is being told about it.
 */
final class VanillaButtonLabel {

    private static final Logger LOG = Global.getLogger(VanillaButtonLabel.class);

    // Says once per session that a button's words could not be reached, rather than on every
    // reattachment. One holder for every way of not finding them, that being one piece of news to
    // whoever asked - the key is bound and unannounced.
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

    // How the row's own buttons wear a key their words do not already contain - "Starscape [1]".
    private static final String SHORTCUT_SUFFIX = " [%s]";

    private final LabelAPI label;

    private VanillaButtonLabel(LabelAPI label) {
        this.label = label;
    }

    /**
     * Finds the words on a button the game built.
     *
     * @param button the widget to look under, which is the game's own rather than anything drawn here
     * @return its words as something that can be read and written, or null where they cannot be
     *         reached - logged once, and never an error, the widget being somebody else's
     */
    static VanillaButtonLabel resolveLabelOf(Object button) {

        try {
            var renderer = readNoArg(button, RENDERER_ACCESSOR);
            var label = findLabelUnder(
                renderer == null ? button : renderer,
                MAX_HOPS_BELOW_RENDERER,
                // By identity: two widgets are the same node here only if they are the same object,
                // and asking a live widget whether it equals another is a question it may answer
                // expensively or not at all.
                Collections.newSetFromMap(new IdentityHashMap<>()));

            if (label == null) {
                WARNING.warnOnce(
                    "The control appended to the map's filter row has no words this can reach, so "
                        + "the key bound to it is not announced on it.");
            }
            return label == null ? null : new VanillaButtonLabel(label);

        } catch (Throwable failure) {

            // Throwable rather than Exception: reading a member resolves every type in its
            // signature, so a shape that has moved arrives as an Error, and the bypass the reads go
            // through lets a checked throw escape unannounced.
            WARNING.warnOnce(
                "The control appended to the map's filter row could not be read for its words, so "
                    + "the key bound to it is not announced on it.",
                failure);
            return null;
        }
    }

    /**
     * Says which key presses the button, in the words the button already wears.
     *
     * <p>The game's own rule, applied to a button the game will not apply it to: a key whose name
     * already occurs in the words has that occurrence lit rather than repeated, and one that does
     * not is spelled out after them in a bracket. That is why the row reads "Starscape [1]" and a
     * core tab reads "Chara(c)ter" - one rule, two outcomes, and matching it is what keeps an
     * appended control from being the one thing on the strip that announces itself differently.
     *
     * @param keyName what the key is called, as the key table names it
     */
    void announceShortcut(String keyName) {

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
    // that leads anywhere - which is exactly what it did.
    //
    // Bounded to the depth the game uses, and every call guarded: a widget asked a question it does
    // not care for answers by throwing, and that is a dead end here rather than a failure.
    private static LabelAPI findLabelUnder(Object node, int hopsLeft, Set<Object> visited) {

        if (node == null || !visited.add(node)) {
            return null;
        }

        if (node instanceof LabelAPI label) {
            return label;
        }

        if (readNoArg(node, TITLE_ACCESSOR) instanceof LabelAPI title) {
            return title;
        }

        return hopsLeft <= 0 ? null : findLabelBelow(node, hopsLeft, visited);
    }

    // One level down, through everything the node holds, first answer winning.
    private static LabelAPI findLabelBelow(Object node, int hopsLeft, Set<Object> visited) {

        for (var method : CoreUiMethods.readPublicMethodsOf(node.getClass())) {

            if (!isFollowableHop(method)) {
                continue;
            }

            var held = readQuietly(method, node);
            var label = findLabelUnder(held, hopsLeft - 1, visited);

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

    // Lights one run of the words in the colour the game lights a key in. Both calls or neither: a
    // run named without a colour draws in whatever the last caller left behind.
    private void lightRun(String run) {

        label.setHighlight(run);

        // The shade the engine gives a key on a button rather than the one it gives an emphasised
        // word: the two match on a stock install and are separate keys, so a restyle that parts them
        // should part this from prose as well.
        label.setHighlightColors(StarsectorUiColour.VANILLA_BUTTON_SHORTCUT.resolve());
    }
}

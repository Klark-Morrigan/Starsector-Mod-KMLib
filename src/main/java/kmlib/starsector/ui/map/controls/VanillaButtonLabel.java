package kmlib.starsector.ui.map.controls;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.LabelAPI;

import kmlib.logging.SessionWarning;
import kmlib.starsector.ui.colour.StarsectorUiColour;
import kmlib.starsector.ui.coreui.CoreUiMethods;
import kmlib.text.KmlibStrings;

import org.apache.log4j.Logger;

import java.util.Locale;

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
            var label = findLabelUnder(renderer == null ? button : renderer);

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

    // Whether a shape answers a title that is drawable words, asked of the type before anything is
    // called on the instance. What makes the hop below the renderer a lookup rather than a trawl:
    // the one method worth calling is the one already declaring where it leads.
    private static boolean declaresTitleAccessor(Class<?> shape) {

        return CoreUiMethods.readPublicMethodsOf(shape)
            .stream()
            .anyMatch(method -> TITLE_ACCESSOR.equals(method.getName())
                    && method.getParameterTypes().isEmpty()
                    && LabelAPI.class.isAssignableFrom(method.getReturnType()));
    }

    // The words under a node: its own title, or the title of the one thing it holds that has one.
    // Two levels is the whole depth the game uses - a button's renderer either draws its own words
    // or holds the piece that does - so a deeper walk would be searching for something that is not
    // there.
    private static LabelAPI findLabelUnder(Object node) {

        if (node == null) {
            return null;
        }

        if (node instanceof LabelAPI label) {
            return label;
        }

        if (readNoArg(node, TITLE_ACCESSOR) instanceof LabelAPI title) {
            return title;
        }

        return findTitleBelow(node);
    }

    // The title of whatever the node holds. Only the members whose own type promises one are called,
    // so a widget is never asked a question to find out whether it was the right question.
    private static LabelAPI findTitleBelow(Object node) {

        for (var method : CoreUiMethods.readPublicMethodsOf(node.getClass())) {

            if (!method.getParameterTypes().isEmpty()
                    || method.getReturnType().isPrimitive()
                    || !declaresTitleAccessor(method.getReturnType())) {
                continue;
            }

            if (readNoArg(method.invokeOn(node), TITLE_ACCESSOR) instanceof LabelAPI title) {
                return title;
            }
        }
        return null;
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

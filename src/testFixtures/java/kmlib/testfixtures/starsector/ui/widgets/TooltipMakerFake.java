package kmlib.testfixtures.starsector.ui.widgets;

import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * A tooltip element that records what was added to it instead of laying anything out, so a suite
 * about what a body is made of can read it back without a running game.
 *
 * <p>A proxy rather than a fixture class: the element API is far too wide to implement by hand, and
 * the handful of calls a body makes - a paragraph, the highlights set on the label it hands back,
 * and a button - are all that a case ever asks about. Every other call answers the zero of its
 * return type, which is what a caller gets from an element it never set up.
 *
 * <p>Highlights are read off the label rather than the paragraph because that is where the engine
 * wants them: {@code addPara} hands back a label, and the tinting is set on it.
 */
public final class TooltipMakerFake {

    // The element API's button call, as the one overload a notice uses: label, ID, width, height
    // and the gap above it. Matched on its shape because the proxy is handed a name and arguments
    // rather than a resolved overload.
    private static final int BUTTON_ARGUMENT_COUNT = 5;
    private static final int BUTTON_GAP_ARGUMENT = 4;

    private final List<AddedParagraph> paragraphs = new ArrayList<>();
    private final List<AddedButton> buttons = new ArrayList<>();

    private TooltipMakerFake() {
    }

    /**
     * @return a recorder with nothing added to it yet
     */
    public static TooltipMakerFake createRecording() {

        return new TooltipMakerFake();
    }

    /**
     * The element itself, to hand to whatever fills it.
     *
     * @return the element, recording into this
     */
    public TooltipMakerAPI asElement() {

        return (TooltipMakerAPI) Proxy.newProxyInstance(
            TooltipMakerFake.class.getClassLoader(),
            new Class<?>[] { TooltipMakerAPI.class },
            (proxy, method, arguments) -> recordElementCall(method.getName(), arguments, method.getReturnType()));
    }

    /**
     * @return the paragraphs added, in the order they were added
     */
    public List<AddedParagraph> readParagraphs() {

        return List.copyOf(paragraphs);
    }

    /**
     * @return the buttons added, in the order they were added
     */
    public List<AddedButton> readButtons() {

        return List.copyOf(buttons);
    }

    // The zero of a return type, for every call a case is not about. Only the primitives the
    // element API answers with are named; everything else is a reference and answers null.
    private static Object resolveDefaultValue(Class<?> returnType) {

        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == boolean.class) {
            return false;
        }
        return null;
    }

    private Object recordElementCall(String methodName, Object[] arguments, Class<?> returnType) {

        if ("addPara".equals(methodName) && arguments != null && arguments.length >= 1) {
            return recordParagraph(arguments);
        }
        if ("addButton".equals(methodName)
                && arguments != null
                && arguments.length == BUTTON_ARGUMENT_COUNT) {

            buttons.add(new AddedButton(
                (String) arguments[0],
                arguments[1],
                (Float) arguments[BUTTON_GAP_ARGUMENT]));

            return null;
        }
        return resolveDefaultValue(returnType);
    }

    // One paragraph, plus the label it hands back so the highlights set on that label land on it.
    private LabelAPI recordParagraph(Object[] arguments) {

        var paragraph = new AddedParagraph(
            (String) arguments[0],
            readGapAbove(arguments),
            new ArrayList<>(),
            new ArrayList<>());

        paragraphs.add(paragraph);

        return (LabelAPI) Proxy.newProxyInstance(
            TooltipMakerFake.class.getClassLoader(),
            new Class<?>[] { LabelAPI.class },
            (proxy, method, labelArguments) ->
                recordLabelCall(paragraph, method.getName(), labelArguments, method.getReturnType()));
    }

    // The gap is the last argument of every addPara overload that takes one, and the only float
    // among them - so it is read by type rather than by counting an overload's arguments.
    private float readGapAbove(Object[] arguments) {

        var lastArgument = arguments[arguments.length - 1];

        return lastArgument instanceof Float gap ? gap : 0f;
    }

    private Object recordLabelCall(
            AddedParagraph paragraph,
            String methodName,
            Object[] arguments,
            Class<?> returnType) {

        if ("setHighlight".equals(methodName) && arguments != null && arguments.length == 1) {
            paragraph.highlightRuns().addAll(List.of((String[]) arguments[0]));
            return null;
        }
        if ("setHighlightColors".equals(methodName) && arguments != null && arguments.length == 1) {
            paragraph.highlightColours().addAll(List.of((Color[]) arguments[0]));
            return null;
        }
        return resolveDefaultValue(returnType);
    }

    /**
     * One paragraph as it was added.
     *
     * @param text             the paragraph's wording
     * @param gapAbove         the padding asked for above it
     * @param highlightRuns    the runs set as highlights on its label, in order
     * @param highlightColours the colours those runs were tinted, in the same order
     */
    public record AddedParagraph(
        String text,
        float gapAbove,
        List<String> highlightRuns,
        List<Color> highlightColours) {
    }

    /**
     * One button as it was added.
     *
     * @param label    what is written on it
     * @param buttonId what it reports itself as when pressed
     * @param gapAbove the padding asked for above it
     */
    public record AddedButton(
        String label,
        Object buttonId,
        float gapAbove) {
    }
}

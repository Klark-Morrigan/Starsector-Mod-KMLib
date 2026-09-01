package kmlib.starsector.ui.map.controls;

import com.fs.starfarer.api.Global;

import kmlib.logging.SessionWarning;
import kmlib.starsector.ui.coreui.CoreUiMethod;
import kmlib.starsector.ui.coreui.CoreUiMethods;

import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.function.Predicate;

/**
 * Stands one more toggle at the end of a map's filter row, built the way the row builds its own and
 * reporting its clicks somewhere else.
 *
 * <p>Everything here is done through signatures rather than names, because the two members of the
 * row that matter - the one that makes a button and the one that puts it at the end - are members
 * the game keeps to itself, and the obfuscator gives them both the same meaningless name and a
 * different one with every build. Their shapes do not move: one takes the words for a button and
 * answers with a button, the other takes a button and the two lengths to lay it at and answers
 * nothing. So the shapes are what is matched, and an ambiguous match is refused rather than guessed
 * at - a row offering two members that both look like its button factory is a row this no longer
 * understands, and the honest answer is to leave it alone.
 *
 * <p>Naming no obfuscated type is what makes the whole of it exercisable. Every widget is an
 * {@code Object} here and every member is reached through {@link CoreUiMethods}, so the code that
 * runs against the game's row is the same code that runs against a row standing in for it - where
 * the alternative would be a class the verifier refuses to load outside the game at all, and a rule
 * about somebody else's widget that could only ever be tried by playing.
 *
 * <p>Redirecting the button is the point of building one at all, not an extra. A button on the row
 * reports its clicks to the row, which answers by rewriting the whole of the game's filter settings
 * from its own eight toggles - so a button left reporting there would spend every click writing the
 * player's map filters back over themselves. The redirect is made before the button is put on the
 * row, so there is never a frame where one stands on screen still reporting to it.
 *
 * <p>The stand-in bound in its place is built over whatever interface the button's own setter names,
 * discovered along with everything else, so no type of the game's is named to bind it either. It is
 * built through {@code java.lang.invoke} rather than through the reflection proxy it resembles, for
 * the reason that shapes this whole class: the game's script classloader denies mod code
 * {@code java.lang.reflect} by name, and the invoke package - which is what a proxy is made of
 * underneath - it allows.
 *
 * <p>Every way this can fail resolves to no button and one line in the log, on the reasoning that a
 * decoration appended to somebody else's widget must not be able to take anything down with it. A
 * caller therefore reads back either a button it can drive or nothing, and has one case to handle.
 */
final class VanillaToggleFactory {

    private static final Logger LOG = Global.getLogger(VanillaToggleFactory.class);

    // The button's own accessor for what its clicks go to. Matched on by name first because the game
    // leaves this one unobfuscated, which makes it a stronger signal than any shape; the shape stays
    // behind it as the answer to the build that finally renames it.
    private static final String SET_LISTENER_METHOD = "setListener";

    // What a caller's callback is run through, the callback arriving as the one shape the platform
    // already has for "something to do later".
    private static final String CALLBACK_METHOD = "run";

    // The shape of the row's button factory: the words for the button, then the keyboard shortcut it
    // answers to, and a button back.
    private static final int BUTTON_FACTORY_PARAMETER_COUNT = 2;
    private static final int LABEL_PARAMETER = 0;

    // The shape of the row's appender: the button, then the width and height to lay it at.
    private static final int ROW_APPENDER_PARAMETER_COUNT = 3;
    private static final int BUTTON_PARAMETER = 0;
    private static final int WIDTH_PARAMETER = 1;
    private static final int HEIGHT_PARAMETER = 2;

    // The shape of the button's listener setter: the one thing its clicks are reported to.
    private static final int LISTENER_PARAMETER_COUNT = 1;
    private static final int LISTENER_PARAMETER = 0;

    // Passed where the row passes one of its own shortcut constants. The game does the same for the
    // one button on the intel screen's band that answers to no key, so it is a supported argument
    // rather than one this gets away with - and reaching the obfuscated constant it would take to
    // pass a real one would add a fragile surface for a digit in a bracket.
    private static final Object NO_SHORTCUT = null;

    // Says once per session that the row is no longer a shape this can write into, rather than on
    // every frame a caller reattaches. Failing to recognise it and failing to write into it are the
    // same news - there is no button - so one of these covers both.
    private static final SessionWarning WARNING = new SessionWarning(LOG);

    private VanillaToggleFactory() {
    }

    /**
     * Builds a toggle the way the row builds its own, points its clicks at the given callback, and
     * stands it at the end of the row.
     *
     * @param row       the row to append to
     * @param label     the words on the button
     * @param width     how wide to lay it, which a caller measures off the row rather than states
     * @param height    how tall to lay it, measured the same way
     * @param onToggled what to run when it is clicked, which is called after the button has already
     *                  flipped its own state, so a caller reads that state rather than tracking it
     * @return the button, for a caller that goes on to drive it, or null when the row is not a shape
     *         this understands or the write into it failed - which is logged once and is not an
     *         error, the row being somebody else's
     */
    static Object appendToggle(
        MapFilterRow row,
        String label,
        float width,
        float height,
        Runnable onToggled) {

        try {
            var rowWidget = row.getRowWidget();

            var rowShape = matchRowShape(rowWidget.getClass());
            if (rowShape == null) {
                return null;
            }

            var button = rowShape.buttonFactory().invokeOn(rowWidget, label, NO_SHORTCUT);
            if (button == null) {
                WARNING.warnOnce(
                    "The map's filter row built no button when asked for one; no control is "
                        + "appended to it.");
                return null;
            }

            // Before the append, so the button is never on screen while still reporting to the row.
            var listener = rowShape.listener();
            listener.setter().invokeOn(button, createListener(listener, onToggled));

            rowShape.rowAppender().invokeOn(rowWidget, button, width, height);

            return button;

        } catch (Throwable cannotAppendToggle) {

            WARNING.warnOnce(
                "Appending a control to the map's filter row failed; the row is left as the game "
                    + "built it.",
                cannotAppendToggle);

            return null;
        }
    }

    // What has to be recognised before anything is built. Read in this order because each answer
    // narrows the next: the factory says what a button is, the button says what its listener is, and
    // the listener says what a stand-in for one has to look like.
    private static RowShape matchRowShape(Class<?> rowShape) {

        var rowMethods = CoreUiMethods.readDeclaredMethodsOf(rowShape);

        var buttonFactory = matchSoleMethod(
            rowMethods,
            VanillaToggleFactory::isButtonFactory,
            "the member of the map's filter row that builds one of its buttons");

        if (buttonFactory == null) {
            return null;
        }

        var buttonShape = buttonFactory.getReturnType();
        var rowAppender = matchSoleMethod(
            rowMethods,
            rowMethod -> isRowAppender(rowMethod, buttonShape),
            "the member of the map's filter row that stands a button at the end of it");

        if (rowAppender == null) {
            return null;
        }

        var listener = matchListenerBinding(buttonShape);

        return listener == null
            ? null
            : new RowShape(buttonFactory, rowAppender, listener);
    }

    // How a button's clicks are taken off the row: the member that rebinds them, the interface that
    // member names, and what that interface's own callback takes - which is the shape a stand-in for
    // it has to be built to. Found together because each is read off the one before it.
    //
    // The member is asked for by name first and by shape second, so an unobfuscated name is used
    // while it lasts and its disappearance costs the match nothing. Only the shape attempt reports a
    // failure: a name that finds nothing is the ordinary way into the fallback, and warning there
    // would spend the session's one warning on a build that went on to work.
    private static ListenerBinding matchListenerBinding(Class<?> buttonShape) {

        var buttonMethods = CoreUiMethods.readPublicMethodsOf(buttonShape);
        var namedSetters = buttonMethods.stream()
            .filter(buttonMethod -> SET_LISTENER_METHOD.equals(buttonMethod.getName()))
            .filter(VanillaToggleFactory::isListenerSetter)
            .toList();

        var listenerSetter = namedSetters.size() == 1
            ? namedSetters.get(0)
            : matchSoleMethod(
                buttonMethods,
                VanillaToggleFactory::isListenerSetter,
                "the member of a filter button that redirects where it reports its clicks");

        if (listenerSetter == null) {
            return null;
        }

        var listenerShape = listenerSetter.getParameterTypes().get(LISTENER_PARAMETER);
        var listenerCallback = matchSoleMethod(
            CoreUiMethods.readPublicMethodsOf(listenerShape),
            "the callback a filter button's listener hears a click through");

        return listenerCallback == null
            ? null
            : new ListenerBinding(
                listenerSetter, listenerShape, listenerCallback.getParameterTypes());
    }

    // The members of a shape that fit, where there has to be exactly one of them.
    private static CoreUiMethod matchSoleMethod(
        List<CoreUiMethod> methods,
        Predicate<CoreUiMethod> isMatch,
        String whatWasWanted) {

        return matchSoleMethod(
            methods.stream()
                .filter(isMatch)
                .toList(),
            whatWasWanted);
    }

    // One member or none. Two are refused for the same reason none is: what is wanted is a
    // particular member, and a set holding two of them says the recognition no longer picks it out -
    // taking either would be a coin toss made inside somebody else's widget.
    //
    // Beside the filtering match above rather than folded into it with a predicate that accepts
    // everything, because an interface having exactly one callback is a question about the set
    // itself rather than about any member's shape.
    private static CoreUiMethod matchSoleMethod(List<CoreUiMethod> matches, String whatWasWanted) {

        if (matches.size() == 1) {
            return matches.get(0);
        }

        WARNING.warnOnce("Found "
            + matches.size()
            + " candidates for "
            + whatWasWanted
            + ", where exactly one "
            + "was expected; no control is appended to the map's filter row.");

        return null;
    }

    // Recognised by taking the words for a button first and answering with something, which is what
    // separates it from everything else the row declares - including the callback it answers its own
    // buttons' clicks through, which takes two arguments as well and neither of them words.
    private static boolean isButtonFactory(CoreUiMethod rowMethod) {

        var parameterTypes = rowMethod.getParameterTypes();

        return parameterTypes.size() == BUTTON_FACTORY_PARAMETER_COUNT
            && parameterTypes.get(LABEL_PARAMETER) == String.class
            && rowMethod.getReturnType() != void.class;
    }

    // Recognised by taking a button and two lengths and answering nothing. The button parameter is
    // tested as accepting one rather than as being one, since the row lays out its children through
    // whatever base type it holds them as.
    private static boolean isRowAppender(CoreUiMethod rowMethod, Class<?> buttonShape) {

        var parameterTypes = rowMethod.getParameterTypes();

        return rowMethod.getReturnType() == void.class
            && parameterTypes.size() == ROW_APPENDER_PARAMETER_COUNT
            && parameterTypes.get(BUTTON_PARAMETER).isAssignableFrom(buttonShape)
            && parameterTypes.get(WIDTH_PARAMETER) == float.class
            && parameterTypes.get(HEIGHT_PARAMETER) == float.class;
    }

    // The parameter being an interface is the load-bearing half. A stand-in can only be built for
    // one, so a setter taking anything else is a button whose clicks cannot be redirected at all,
    // whatever it is called.
    private static boolean isListenerSetter(CoreUiMethod buttonMethod) {

        var parameterTypes = buttonMethod.getParameterTypes();

        return buttonMethod.getReturnType() == void.class
            && parameterTypes.size() == LISTENER_PARAMETER_COUNT
            && parameterTypes.get(LISTENER_PARAMETER).isInterface();
    }

    // The stand-in listener: the caller's callback, widened to whatever the listener's own callback
    // takes. The arguments are dropped rather than passed on because the only two a click carries
    // are what happened and which button it happened to, and a listener bound to one button knows
    // both already.
    private static Object createListener(ListenerBinding listener, Runnable onToggled)
        throws Throwable {

        var onToggledHandle = MethodHandles.lookup()
            .findVirtual(Runnable.class, CALLBACK_METHOD, MethodType.methodType(void.class))
            .bindTo(onToggled);

        return MethodHandleProxies.asInterfaceInstance(
            listener.shape(),
            MethodHandles.dropArguments(onToggledHandle, 0, listener.callbackParameterTypes()));
    }

    // Everything one write into a row needs, matched together because each part is found through the
    // one before it: a half-matched row is not something to hold, and carrying the parts separately
    // would let a caller pair a button factory with the appender of some other row.
    private record RowShape(
        CoreUiMethod buttonFactory,
        CoreUiMethod rowAppender,
        ListenerBinding listener) {
    }

    // The listener half of that, kept together for the same reason and apart from the rest because
    // it is about the button rather than about the row: the interface and its callback's shape are
    // both read off the setter, and a caller holding the setter alone would have to read them again
    // to use it.
    private record ListenerBinding(
        CoreUiMethod setter,
        Class<?> shape,
        List<Class<?>> callbackParameterTypes) {
    }
}

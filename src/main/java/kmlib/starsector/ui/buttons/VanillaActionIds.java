package kmlib.starsector.ui.buttons;

import com.fs.starfarer.api.ui.ButtonAPI;

/**
 * The ID a caller put on a vanilla widget, found again in the pair of objects the game hands its action
 * listener.
 *
 * <p>It exists because that pair is not one shape. A {@code TooltipMakerAPI.ActionListenerDelegate} is
 * called with two objects and the API names neither, and what arrives in each depends on the widget: the
 * engine's own panel treats the second as the widget and reads the ID off it
 * ({@code getCustomData()}), while other surfaces hand the ID straight over. A caller that reads one
 * position and tests it against its own ID type therefore works for some of its controls and silently does
 * nothing for the rest - the press is delivered, the test fails, and nothing reports it.
 *
 * <p>So the ID is looked for in every place it is known to arrive, in the order that costs least: handed
 * over directly in either position, or carried by a widget in either. Nothing here guesses - each of the
 * three is a shape the engine actually produces - and a press carrying no ID of the caller's kind answers
 * null, which is the honest answer for a widget somebody else added.
 *
 * <p>Typed on the ID rather than answering {@code Object}, because the caller's next move is always to
 * act on its own enum or record and a resolver returning the raw object would push the same cast back out
 * to every call site - which is the fault this exists to fix, one step further along.
 */
public final class VanillaActionIds {

    private VanillaActionIds() {
    }

    /**
     * Finds the caller's own ID among the two objects an action delegate was handed.
     *
     * @param firstArgument  the delegate's first object
     * @param secondArgument the delegate's second object
     * @param idType         the kind of ID the caller put on its widgets
     * @param <T>            that kind
     * @return the ID, or null when neither object carries one of that kind - a press on a widget the
     *         caller did not add, or one it added without an ID
     */
    public static <T> T resolveActionId(Object firstArgument, Object secondArgument, Class<T> idType) {

        var directId = readIdOf(firstArgument, idType);
        if (directId != null) {
            return directId;
        }
        var secondId = readIdOf(secondArgument, idType);
        if (secondId != null) {
            return secondId;
        }
        var widgetId = readWidgetIdOf(firstArgument, idType);

        return widgetId != null
            ? widgetId
            : readWidgetIdOf(secondArgument, idType);
    }

    // The ID handed over as itself, which is what a surface passing the caller's own value produces.
    private static <T> T readIdOf(Object argument, Class<T> idType) {

        return idType.isInstance(argument)
            ? idType.cast(argument)
            : null;
    }

    // The ID carried by the widget, which is what the engine's own panel produces: it passes the button
    // and expects the reader to take the data off it.
    private static <T> T readWidgetIdOf(Object argument, Class<T> idType) {

        return argument instanceof ButtonAPI button
            ? readIdOf(button.getCustomData(), idType)
            : null;
    }
}

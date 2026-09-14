package kmlib.testfixtures.starsector.ui.map.controls;

/**
 * What the game's filter buttons report a click to: one interface with a single two-argument
 * callback, which the row itself implements and every button on it is bound to. Shipped from KMLib
 * so both KMLib's and consuming mods' tests stand a row on the same shape.
 *
 * <p>An interface rather than a class because that is the half of the shape that matters. A control
 * appended to somebody else's row has to divert one button's reports to itself, and what makes that
 * possible is the parameter being an interface at all - anything else could not be stood in for
 * without naming the game's own type.
 */
public interface MapFilterActionListenerFake {

    /**
     * @param action    what happened, which the game passes as its own untyped action token
     * @param component the button it happened on
     */
    void actionPerformed(Object action, Object component);
}

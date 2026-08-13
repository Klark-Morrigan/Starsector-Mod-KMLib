package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import org.mockito.Mockito;

/**
 * The engine pointer events the panel controllers are driven with. Shared between the suites over the two
 * controllers because what they build is not a fixture either owns: which predicate the engine sets for a
 * press, and which sign it reports a wheel turned down with, are facts about the input API - and a fact
 * spelt out in two suites is a fact the two can come to disagree about, each passing against its own
 * spelling.
 *
 * <p>Only what the point and the kind of event require is stubbed. A controller case is about what the
 * panel does with an event, so an event carrying more than the case names would let a branch nobody wrote
 * a case for be the one that answered.
 */
final class PointerEventMocks {

    // The raw value the engine reports for a wheel turned toward the bottom of a list. Only the sign is
    // read by the panel, so the magnitude is immaterial - and the panel scrolls the list the other way from
    // it, which is the convention that has to be stated once rather than assumed twice.
    private static final int WHEEL_DOWN = -1;

    private PointerEventMocks() {
    }

    /**
     * A mouse event at a point and nothing more - no button, no wheel - for a case about what the panel
     * claims rather than about what it does with a press. The point is rounded on the way in, an engine
     * event reporting whole pixels where the placement it is tested against is laid out in floats.
     *
     * @param pointX the pointer's x, in the UI coordinates a placement is laid out in
     * @param pointY the pointer's y, in UI coordinates
     * @return the event
     */
    static InputEventAPI mockPointerEventAt(float pointX, float pointY) {

        var eventMock = Mockito.mock(InputEventAPI.class);

        Mockito
            .when(eventMock.getX())
            .thenReturn(Math.round(pointX));
        Mockito
            .when(eventMock.getY())
            .thenReturn(Math.round(pointY));

        return eventMock;
    }

    /**
     * A left-button press at a point, for the parts of a panel that act on the way down - the collapse
     * handle, a body control, the scrollbar's grab column.
     *
     * @param pointX the press x, in UI coordinates
     * @param pointY the press y, in UI coordinates
     * @return the event
     */
    static InputEventAPI mockLeftPressAt(float pointX, float pointY) {

        var eventMock = mockPointerEventAt(pointX, pointY);

        Mockito
            .when(eventMock.isLMBDownEvent())
            .thenReturn(true);

        return eventMock;
    }

    /**
     * A left-button release at a point. The point is carried because the panel is handed one - a release
     * reports where the button came up - even though what the tabs do with it is deliberately blind to it.
     *
     * @param pointX the release x, in UI coordinates
     * @param pointY the release y, in UI coordinates
     * @return the event
     */
    static InputEventAPI mockLeftReleaseAt(float pointX, float pointY) {

        var eventMock = mockPointerEventAt(pointX, pointY);

        Mockito
            .when(eventMock.isLMBUpEvent())
            .thenReturn(true);

        return eventMock;
    }

    /**
     * A wheel notch turned toward the bottom of the list, at a point.
     *
     * @param pointX the pointer's x, in UI coordinates
     * @param pointY the pointer's y, in UI coordinates
     * @return the event
     */
    static InputEventAPI mockWheelDownAt(float pointX, float pointY) {

        var eventMock = mockPointerEventAt(pointX, pointY);

        Mockito
            .when(eventMock.isMouseScrollEvent())
            .thenReturn(true);
        Mockito
            .when(eventMock.getEventValue())
            .thenReturn(WHEEL_DOWN);

        return eventMock;
    }
}

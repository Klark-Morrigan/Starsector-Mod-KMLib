package kmlib.testfixtures.starsector.ui.map.controls;

/**
 * One toggle on the game's map filter row: a label, a checked state, and the listener its clicks are
 * reported to. Shipped from KMLib so both KMLib's and consuming mods' tests build the same shape of
 * row.
 *
 * <p>Its own type rather than a bare object, because the row's two helpers are told apart by the
 * types in their signatures rather than by their names - the game gives them the same name - so a
 * button that were an {@code Object} would leave the appender indistinguishable from anything else
 * taking one.
 *
 * <p>The listener is replaceable, which is the property the whole arrangement exists to model:
 * setting one detaches the button from the row that built it, so the row stops rewriting the game's
 * filter settings on every click of it.
 */
public final class MapFilterButtonFake {

    private final String label;

    private boolean isChecked;

    private MapFilterActionListenerFake listener;

    /**
     * @param label    the words on the button
     * @param listener what its clicks are reported to, which is the row that built it
     */
    public MapFilterButtonFake(String label, MapFilterActionListenerFake listener) {
        this.label = label;
        this.listener = listener;
    }

    /**
     * @return what its clicks are currently reported to
     */
    public MapFilterActionListenerFake readListener() {
        return listener;
    }

    /**
     * @return the words on the button
     */
    public String readLabel() {
        return label;
    }

    public boolean isChecked() {
        return isChecked;
    }

    /** Reports a click the way the game's own button does, so a diverted listener can be observed. */
    public void click() {
        isChecked = !isChecked;
        if (listener != null) {
            listener.actionPerformed(null, this);
        }
    }

    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    public void setListener(MapFilterActionListenerFake listener) {
        this.listener = listener;
    }
}

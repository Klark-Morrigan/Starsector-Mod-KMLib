package kmlib.testfixtures.starsector.ui.map.probes;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.testfixtures.starsector.ui.coreui.CoreUiWidgetFake;
import kmlib.testfixtures.starsector.ui.map.BaseSectorMapFake;

import java.util.List;

/**
 * A sector map that is also a placed, drawn widget - the shape a mod's composited minimap actually
 * has, and the one a rule about where such a map is on screen has to be driven against. Shipped from
 * KMLib so both KMLib's and consuming mods' tests build the same shape of tree.
 *
 * <p>{@link SectorMapWidgetFake} stands for a map a walk only has to recognise, and deliberately
 * answers nothing about layout so a rule depending on layout cannot pass while testing only
 * recognition. This is the counterpart for the rules that do depend on it.
 *
 * <p>The widget half is delegated to {@link CoreUiWidgetFake} rather than written out again, so
 * "placed, drawn component" means one thing across the fixtures: a case that moved this one's
 * opacity threshold or its unpositioned answer without moving the other's would be describing two
 * different screens.
 */
public final class PlacedSectorMapWidgetFake extends BaseSectorMapFake implements UIComponentAPI {

    private final CoreUiWidgetFake widgetFake;

    /**
     * @param position the box the layout placed it in, or null for a map that was never positioned
     * @param opacity  how solidly it is drawn, on the same scale the live components report
     * @param children what hangs under it, which a walk descends into
     */
    public PlacedSectorMapWidgetFake(PositionAPI position, float opacity, Object... children) {
        widgetFake = new CoreUiWidgetFake(position, opacity, children);
    }

    public List<Object> getChildrenCopy() {
        return widgetFake.getChildrenCopy();
    }

    @Override
    public float getOpacity() {
        return widgetFake.getOpacity();
    }

    @Override
    public PositionAPI getPosition() {
        return widgetFake.getPosition();
    }

    @Override
    public void setOpacity(float opacity) {
        widgetFake.setOpacity(opacity);
    }

    @Override
    public void advance(float amount) {
        widgetFake.advance(amount);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        widgetFake.processInput(events);
    }

    @Override
    public void render(float alphaMult) {
        widgetFake.render(alphaMult);
    }
}

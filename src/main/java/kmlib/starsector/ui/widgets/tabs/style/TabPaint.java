package kmlib.starsector.ui.widgets.tabs.style;

/**
 * Everything one tab is painted with, once its channels have been resolved: the surface it settled on -
 * its state's look, moved by whatever lift it carries - and the light to lay over the finished result.
 * What a chrome is handed per tab, so the walk that resolves the channels stays the one place they are
 * composed and a chrome is left with painting alone.
 *
 * <p>Two fields rather than one because they reach the screen at different moments: the look is drawn, and
 * the light is added over whatever the drawing produced, including the text on it and whatever shows
 * through a surface a chrome left unpainted. Folded together, a chrome could only apply the light to the
 * fill it painted, which is precisely the dilution the light exists to avoid.
 *
 * @param look  the surface the tab settled on, its lift included
 * @param light the light to add over it, or {@link TabLight#NONE} where the row's pointer rule brightens
 *              by shade instead
 */
public record TabPaint(
    TabLook look,
    TabLight light) {
}

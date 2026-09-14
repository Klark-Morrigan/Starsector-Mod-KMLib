package kmlib.starsector.ui.text;

/**
 * A painter that draws nothing and remembers what it was handed: which kind of run reached it, and at
 * what anchor. Stands in for a drawing surface because what {@link LabelRun#paintRun} promises is a
 * dispatch - each run naming its own kind - and a subject that painted would answer that only through
 * GL calls no suite can watch.
 */
final class LabelRunPainterFake implements LabelRunPainter {

    // What no run has reached yet, for both halves of the record. Named so an unvisited painter reads as
    // one rather than as a bare null and a bare zero.
    private static final LabelRun NO_PAINTED_RUN = null;
    private static final float NO_PAINTED_RUN_X = 0f;

    private LabelRun paintedRun = NO_PAINTED_RUN;
    private float paintedRunX = NO_PAINTED_RUN_X;

    LabelRun getPaintedRun() {
        return paintedRun;
    }

    float getPaintedRunX() {
        return paintedRunX;
    }

    @Override
    public void paintImageSpan(ImageSpan imageSpan, float runX) {
        recordPaintedRun(imageSpan, runX);
    }

    @Override
    public void paintRedactedSpan(RedactedSpan redactedSpan, float runX) {
        recordPaintedRun(redactedSpan, runX);
    }

    @Override
    public void paintTextSpan(TextSpan textSpan, float runX) {
        recordPaintedRun(textSpan, runX);
    }

    // Keeps the run as the kind that reached it, so an assertion reads both which method was called - the
    // run's own type - and that the run handed on was the one that dispatched.
    private void recordPaintedRun(LabelRun labelRun, float runX) {
        paintedRun = labelRun;
        paintedRunX = runX;
    }
}

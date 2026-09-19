package kmlib.starsector.ui.controls.specs;

import java.util.List;

/**
 * A horizontal rule spanning the strip's inner width, drawn but never clicked - it parts one run
 * of controls from the next. It is not {@link InteractiveSpec} and carries no label; the layout spans
 * it to the content width and the renderer draws the rule.
 */
public record DividerSpec() implements ControlSpec {

    @Override
    public List<String> labels() {
        return List.of();
    }
}

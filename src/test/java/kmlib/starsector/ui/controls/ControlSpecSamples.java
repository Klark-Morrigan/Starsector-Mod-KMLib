package kmlib.starsector.ui.controls;

import kmlib.starsector.ui.controls.specs.ControlAction;
import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.DividerSpec;
import kmlib.starsector.ui.controls.specs.HorizontalRadioSpec;
import kmlib.starsector.ui.controls.specs.ReselectBehaviour;
import kmlib.starsector.ui.controls.specs.ScrollingSectionSpec;
import kmlib.starsector.ui.controls.specs.SideBySideSpec;
import kmlib.starsector.ui.controls.specs.TabsSpec;
import kmlib.starsector.ui.controls.specs.VerticalRadioSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One representative of every variant the sealed spec set holds, keyed by the variant it stands for.
 *
 * <p>The catalogue a reader of the whole set is driven from. A rule that must hold for every variant
 * - that each measures to a width, that a control declaring separate cells is given them, that a
 * group expands into what it holds - cannot be stated against a hand-written case per variant,
 * because the case nobody wrote is exactly the one that would have failed. Stated against this
 * instead, a variant added to the set arrives in every such rule at once.
 *
 * <p>What makes that work is the completeness check its readers run first: the keys here are held
 * against the variants read off the sealed type itself, so a variant with no sample fails before any
 * rule is asked about it. The catalogue is then a list nobody can quietly fall off.
 *
 * <p>Each sample is the plainest shape of its variant. A group holds two children rather than one,
 * that being what makes "expands into more than one control" observable at all.
 *
 * <p>Final class with a private constructor: catalogue of static builders, no instances.
 */
public final class ControlSpecSamples {

    private static final String FIRST_OPTION = "Factions";
    private static final String SECOND_OPTION = "Alliances";

    private ControlSpecSamples() {
        // catalogue of static builders, no instances.
    }

    /**
     * Whether a variant stands for a group of controls rather than for one control.
     *
     * <p>Read off the record's own components rather than from a list kept here, so a group added to
     * the set is recognised as one without this being edited. A variant holding a run of specs is a
     * group by construction: there is nothing else such a component could mean.
     *
     * @param variant the variant to describe
     * @return whether it carries a run of child specs
     */
    public static boolean holdsChildSpecs(Class<?> variant) {

        var childRun = List.class.getName() + "<" + ControlSpec.class.getName() + ">";

        for (var component : variant.getRecordComponents()) {
            if (childRun.equals(component.getGenericType().getTypeName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * One sample per variant, in the order the set declares them.
     *
     * @return the catalogue, keyed by the variant each sample stands for
     */
    public static Map<Class<? extends ControlSpec>, ControlSpec> mapSamplesByVariant() {

        var samples = new LinkedHashMap<Class<? extends ControlSpec>, ControlSpec>();

        addSample(samples, LabelledControlSpecs.buildCheckbox("Muted", true, ControlAction.NONE));
        addSample(samples, LabelledControlSpecs.buildToggle("Borders", true, ControlAction.NONE));
        addSample(samples, LabelledControlSpecs.buildLabel("Names"));
        addSample(samples, new DividerSpec());
        addSample(samples, HorizontalRadioSpec.of(
            List.of(FIRST_OPTION, SECOND_OPTION),
            ControlSpec.NO_SELECTION,
            ControlAction.NONE));
        addSample(samples, VerticalRadioSpec.of(
            List.of(FIRST_OPTION, SECOND_OPTION),
            ControlSpec.NO_SELECTION,
            ControlAction.NONE));
        addSample(samples, VerticalTableSpecs.buildSegmentedList(
            List.of(FIRST_OPTION, SECOND_OPTION),
            ControlSpec.NO_SELECTION,
            ControlAction.NONE,
            ReselectBehaviour.INERT));
        addSample(samples, new TabsSpec(
            List.of("Systems", "Fleets"),
            List.of("", ""),
            0,
            ControlAction.NONE));
        addSample(samples, new SideBySideSpec(
            List.of(buildChild("Left")),
            List.of(buildChild("Right"))));
        addSample(samples, new ScrollingSectionSpec(
            List.of(buildChild("First"), buildChild("Second"))));

        return samples;
    }

    /**
     * The concrete variants under a sealed type, flattening the sealed interfaces between them.
     *
     * <p>Those interfaces are groupings a reader names, not controls a host builds, so a rule stated
     * per variant has nothing to say about one.
     *
     * @param sealedType the type to read
     * @return every leaf variant beneath it
     */
    public static List<Class<?>> readLeafVariants(Class<?> sealedType) {

        var variants = new ArrayList<Class<?>>();

        for (var permitted : sealedType.getPermittedSubclasses()) {
            if (permitted.isInterface()) {
                variants.addAll(readLeafVariants(permitted));
            } else {
                variants.add(permitted);
            }
        }
        return variants;
    }

    // Keyed by the sample's own class, so the catalogue cannot name one variant and hold another.
    private static void addSample(
            Map<Class<? extends ControlSpec>, ControlSpec> samples,
            ControlSpec sample) {

        samples.put(sample.getClass(), sample);
    }

    // What a group is filled with: the plainest control there is, since what a group holds is not
    // what any rule about groups is asking.
    private static ControlSpec buildChild(String label) {

        return LabelledControlSpecs.buildCheckbox(label, false, ControlAction.NONE);
    }
}

package kmlib.starsector.ui.controls;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two radio alignments: a row of segments laid side by side and a column of cells
 * stacked, what each carries that the other has no room for (segment sizing and a trailing
 * caption are the row's alone), and the re-pick rule both answer through the radio interface a
 * reader names instead of either alignment.
 */
final class ControlSpecRadioTest {

    @Nested
    class HorizontalRadioConstruction {

        @Test
        void constructorCopiesTheLabelListDefensively() {
            // The canonical constructor is public on a record, so a host can reach it directly; the copy
            // has to live there rather than in the factory for a caller's later edit not to reach the spec.
            var sourceLabels = new ArrayList<>(List.of("Short", "Full"));
            var radio = new ControlSpec.HorizontalRadio(
                sourceLabels,
                0,
                ControlAction.NONE,
                "",
                SegmentSizing.UNIFORM,
                ReselectBehaviour.INERT);

            sourceLabels.add("Mutated");

            assertThat(radio.labels())
                .containsExactly("Short", "Full");
        }
    }

    @Nested
    class HorizontalRadioOf {

        @Test
        void ofIsAUniformInertRadioCarryingNoCaption() {
            // The plain option row a host reaches for by default: cells all the widest label's width, no
            // trailing caption, and always one lit (a re-pick of the lit segment does nothing).
            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE);

            assertThat(radio.segmentSizing())
                .isEqualTo(SegmentSizing.UNIFORM);
            assertThat(radio.reselect())
                .isEqualTo(ReselectBehaviour.INERT);
            assertThat(radio.trailingLabel())
                .isEqualTo(ControlSpec.NO_TRAILING_CAPTION);
            assertThat(radio.hasTrailingCaption())
                .isFalse();
            assertThat(radio.labels())
                .containsExactly("Short", "Full");
            assertThat(radio.selectedIndex())
                .isZero();
        }

        @Test
        void ofCarriesTheClickActionByOptionIndex() {

            var firedCell = new int[] {-99};
            var radio = ControlSpec.HorizontalRadio.of(
                List.of("Short", "Full"),
                0,
                cell -> firedCell[0] = cell);

            radio.action().activateCell(1);

            assertThat(firedCell[0])
                .isEqualTo(1);
        }

        @Test
        void ofComposesWithEveryRefinementAtOnce() {
            // The three refinements are independent axes, so a host reaches combinations no single factory
            // names - here a captioned, snapped, clearable row all at once.
            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE)
                .showsCaption("Names")
                .sizesSegments(SegmentSizing.SNAPPED)
                .handlesReselect(ReselectBehaviour.DESELECT);

            assertThat(radio.trailingLabel())
                .isEqualTo("Names");
            assertThat(radio.segmentSizing())
                .isEqualTo(SegmentSizing.SNAPPED);
            assertThat(radio.reselect())
                .isEqualTo(ReselectBehaviour.DESELECT);
        }
    }

    @Nested
    class VerticalRadioConstruction {

        @Test
        void constructorCopiesTheLabelListDefensively() {
            // The canonical constructor is public on a record, so the copy has to live there rather than
            // in the factory for a caller's later edit not to reach the spec.
            var sourceLabels = new ArrayList<>(List.of("Factions", "Alliances"));
            var radio = new ControlSpec.VerticalRadio(
                sourceLabels,
                0,
                ControlAction.NONE,
                ReselectBehaviour.INERT);

            sourceLabels.add("Mutated");

            assertThat(radio.labels())
                .containsExactly("Factions", "Alliances");
        }
    }

    @Nested
    class VerticalRadioOf {

        @Test
        void ofIsAnInertStackCarryingItsOptionsInOrder() {
            // The plain stacked column a host reaches for by default: always one lit, so a re-pick of the
            // lit cell does nothing.
            var radio = ControlSpec.VerticalRadio.of(
                List.of("Factions", "Alliances", "Claims"),
                1,
                ControlAction.NONE);

            assertThat(radio.reselect())
                .isEqualTo(ReselectBehaviour.INERT);
            assertThat(radio.labels())
                .containsExactly("Factions", "Alliances", "Claims");
            assertThat(radio.selectedIndex())
                .isEqualTo(1);
        }

        @Test
        void ofCarriesTheClickActionByOptionIndex() {

            var firedCell = new int[] {-99};
            var radio = ControlSpec.VerticalRadio.of(
                List.of("Factions", "Alliances", "Claims"),
                0,
                cell -> firedCell[0] = cell);

            radio.action().activateCell(2);

            assertThat(firedCell[0])
                .isEqualTo(2);
        }
    }

    @Nested
    class VerticalRadioHandlesReselect {

        @Test
        void handlesReselectSetsOnlyTheReselectBehaviour() {

            var radio = ControlSpec.VerticalRadio.of(List.of("Factions", "Alliances"), 0, ControlAction.NONE)
                .handlesReselect(ReselectBehaviour.DESELECT);

            assertThat(radio.reselect())
                .isEqualTo(ReselectBehaviour.DESELECT);
            assertThat(radio.labels())
                .containsExactly("Factions", "Alliances");
            assertThat(radio.selectedIndex())
                .isZero();
        }
    }

    @Nested
    class RadioReselect {

        @Test
        void reselectIsReadableOffEitherAlignmentThroughTheRadioType() {
            // The re-pick rule belongs to the control rather than to how its cells are arranged, so a
            // reader that acts on any radio - the activation path - asks the interface and never branches.
            List<ControlSpec.Radio> radios = List.of(
                ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE)
                    .handlesReselect(ReselectBehaviour.DESELECT),
                ControlSpec.VerticalRadio.of(List.of("Factions", "Alliances"), 0, ControlAction.NONE)
                    .handlesReselect(ReselectBehaviour.DESELECT));

            assertThat(radios)
                .extracting(ControlSpec.Radio::reselect)
                .containsExactly(ReselectBehaviour.DESELECT, ReselectBehaviour.DESELECT);
        }
    }

    @Nested
    class HorizontalRadioHasTrailingCaption {

        @Test
        void hasTrailingCaptionIsFalseOnAPlainRow() {

            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE);

            assertThat(radio.hasTrailingCaption())
                .isFalse();
        }

        @Test
        void hasTrailingCaptionIsTrueOnACaptionedRow() {

            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE)
                .showsCaption("Names");

            assertThat(radio.hasTrailingCaption())
                .isTrue();
        }

        @Test
        void hasTrailingCaptionIsFalseOnAWhitespaceOnlyCaption() {
            // A host that assembles a caption from parts and comes up with only spacing gets the
            // uncaptioned row, so the layout reserves no footprint the renderer then draws nothing in.
            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE)
                .showsCaption("   ");

            assertThat(radio.hasTrailingCaption())
                .isFalse();
        }
    }

    @Nested
    class HorizontalRadioShowsCaption {

        @Test
        void showsCaptionSetsOnlyTheTrailingLabel() {

            var radio = ControlSpec.HorizontalRadio.of(List.of("Short", "Full"), 0, ControlAction.NONE)
                .showsCaption("Names");

            assertThat(radio.trailingLabel())
                .isEqualTo("Names");
            assertThat(radio.segmentSizing())
                .isEqualTo(SegmentSizing.UNIFORM);
            assertThat(radio.reselect())
                .isEqualTo(ReselectBehaviour.INERT);
            assertThat(radio.labels())
                .containsExactly("Short", "Full");
            assertThat(radio.selectedIndex())
                .isZero();
        }
    }
}

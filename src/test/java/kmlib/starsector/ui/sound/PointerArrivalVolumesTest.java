package kmlib.starsector.ui.sound;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the balance itself: which level each kind of arrival answers at, and what a host gets by naming
 * none. Both faults here are inaudible in the ordinary sense - a kind wired to its neighbour's level is
 * heard, at best, as a panel that feels off - so the positions are pinned apart rather than as a set.
 */
final class PointerArrivalVolumesTest {

    private static final float LOUD_VOLUME = 0.9f;
    private static final float MIDDLING_VOLUME = 0.6f;
    private static final float NEGATIVE_VOLUME = -0.5f;
    private static final float QUIET_VOLUME = 0.1f;

    @Nested
    class Constructor {

        @Test
        void constructorRejectsANegativeVolumeWhicheverKindNamesIt() {
            // Caught where the balance is composed rather than where a cue is built from it: a look is
            // composed once, and its cue is built on the frame the pointer first reaches something - so the
            // same bad number reaches the player as a crash mid-hover if it is left that late.
            //
            // All three positions, not one standing for the rest: the guard is written once per volume, so
            // a check dropped from one of them is a fault no case about its neighbours can see.
            assertThatThrownBy(() ->
                    new PointerArrivalVolumes(NEGATIVE_VOLUME, MIDDLING_VOLUME, QUIET_VOLUME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volume");

            assertThatThrownBy(() ->
                    new PointerArrivalVolumes(LOUD_VOLUME, NEGATIVE_VOLUME, QUIET_VOLUME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volume");

            assertThatThrownBy(() ->
                    new PointerArrivalVolumes(LOUD_VOLUME, MIDDLING_VOLUME, NEGATIVE_VOLUME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volume");
        }
    }

    @Nested
    class CreateDefaultVolumes {

        @Test
        void createDefaultVolumesTakesTheLibrarysOwnBalance() {
            // Spelt as literals rather than read off the constants they come from, so a default nudged in
            // the source is a decision this case reports rather than one it agrees with silently. The ratio
            // is the point: a listed item is quieter than either thing the player aims at, which is the
            // whole of what stops a column of them chattering under one sweep.
            var arrivalVolumes = PointerArrivalVolumes.createDefaultVolumes();

            assertThat(arrivalVolumes.panelChromeVolume())
                .isEqualTo(0.5f);
            assertThat(arrivalVolumes.singleOptionControlVolume())
                .isEqualTo(0.5f);
            assertThat(arrivalVolumes.listedItemVolume())
                .isEqualTo(0.25f);
        }
    }

    @Nested
    class ResolveVolumeFor {

        @Test
        void resolveVolumeForAnswersEachKindAtTheLevelItWasBuiltWith() {
            // Three distinct numbers rather than the defaults, because a balance agreeing with the ones the
            // library ships would pass whether or not the kind was ever read - and the fault worth catching
            // is a kind wired to a neighbour's level, which no default set can show.
            var arrivalVolumes = new PointerArrivalVolumes(LOUD_VOLUME, MIDDLING_VOLUME, QUIET_VOLUME);

            assertThat(arrivalVolumes.resolveVolumeFor(PointerArrivalTarget.PANEL_CHROME))
                .isEqualTo(LOUD_VOLUME);
            assertThat(arrivalVolumes.resolveVolumeFor(PointerArrivalTarget.SINGLE_OPTION_CONTROL))
                .isEqualTo(MIDDLING_VOLUME);
            assertThat(arrivalVolumes.resolveVolumeFor(PointerArrivalTarget.LISTED_ITEM))
                .isEqualTo(QUIET_VOLUME);
        }
    }
}

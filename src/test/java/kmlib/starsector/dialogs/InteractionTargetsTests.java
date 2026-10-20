package kmlib.starsector.dialogs;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the target reads: a dialog's target answers its ID or itself as a planet, and a missing dialog or target
 * answers nothing.
 */
final class InteractionTargetsTests {

    @Nested
    class FindTargetPlanet {

        @Test
        void findsAPlanetTarget() {

            var planetMock = mock(PlanetAPI.class);
            var dialogMock = mock(InteractionDialogAPI.class);

            when(dialogMock.getInteractionTarget())
                .thenReturn(planetMock);

            assertThat(InteractionTargets.findTargetPlanet(dialogMock))
                .containsSame(planetMock);
        }

        @Test
        void answersNothingForATargetThatIsNoPlanet() {

            var dialogMock = mock(InteractionDialogAPI.class);

            when(dialogMock.getInteractionTarget())
                .thenReturn(mock(SectorEntityToken.class));

            assertThat(InteractionTargets.findTargetPlanet(dialogMock))
                .isEmpty();
        }

        @Test
        void answersNothingForADialogWithoutATarget() {

            assertThat(InteractionTargets.findTargetPlanet(mock(InteractionDialogAPI.class)))
                .isEmpty();
        }

        @Test
        void answersNothingWithoutADialog() {

            assertThat(InteractionTargets.findTargetPlanet(null))
                .isEmpty();
        }
    }

    @Nested
    class ReadTargetId {

        @Test
        void readsTheTargetsId() {

            var targetMock = mock(SectorEntityToken.class);
            var dialogMock = mock(InteractionDialogAPI.class);

            when(targetMock.getId())
                .thenReturn("jangala");
            when(dialogMock.getInteractionTarget())
                .thenReturn(targetMock);

            assertThat(InteractionTargets.readTargetId(dialogMock))
                .isEqualTo("jangala");
        }

        // An unstubbed dialog answers no target, as a dialog open on no entity does.
        @Test
        void answersNothingForADialogWithoutATarget() {

            assertThat(InteractionTargets.readTargetId(mock(InteractionDialogAPI.class)))
                .isNull();
        }

        @Test
        void answersNothingWithoutADialog() {

            assertThat(InteractionTargets.readTargetId(null))
                .isNull();
        }
    }
}

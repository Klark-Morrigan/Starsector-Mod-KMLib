package kmlib.testfixtures.starsector.dialogs;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Pins that the stand-in dialog answers its own panels, a supplied text panel, and the target it is opened on.
 */
final class InteractionDialogMockTests {

    @Nested
    class CreateDialogMock {

        @Test
        void answersItsOwnPanels() {

            var dialog = InteractionDialogMock.createDialogMock();

            assertThat(dialog.getDialog().getOptionPanel())
                .isSameAs(dialog.getOptionPanel());
            assertThat(dialog.getDialog().getTextPanel())
                .isSameAs(dialog.getTextPanel());
            assertThat(dialog.getDialog().getVisualPanel())
                .isSameAs(dialog.getVisualPanel());
        }

        @Test
        void opensOnNoTarget() {

            assertThat(InteractionDialogMock.createDialogMock().getDialog().getInteractionTarget())
                .isNull();
        }
    }

    @Nested
    class CreateDialogMockWithTextPanel {

        @Test
        void answersTheSuppliedTextPanel() {

            var textPanelMock = mock(TextPanelAPI.class);

            assertThat(InteractionDialogMock.createDialogMockWithTextPanel(textPanelMock).getDialog().getTextPanel())
                .isSameAs(textPanelMock);
        }
    }

    @Nested
    class SetTarget {

        @Test
        void answersTheTargetItIsOpenedOn() {

            var targetMock = mock(SectorEntityToken.class);

            assertThat(InteractionDialogMock.createDialogMock().setTarget(targetMock).getDialog().getInteractionTarget())
                .isSameAs(targetMock);
        }
    }
}

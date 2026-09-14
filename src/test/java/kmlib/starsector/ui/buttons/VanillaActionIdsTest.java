package kmlib.starsector.ui.buttons;

import com.fs.starfarer.api.ui.ButtonAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins where a widget's own ID is looked for among the two objects a vanilla action delegate is handed.
 * The API names neither position and the engine fills them differently per widget, so a reader that
 * committed to one is a reader that answers some of its controls and silently drops the rest - which is a
 * press that arrives, fails a test, and reports nothing.
 */
final class VanillaActionIdsTest {

    // The caller's own kind of ID, and a value of another kind for the cases about a press that is not
    // this caller's to act on.
    private enum RowAction {
        MOVE_UP,
        MOVE_DOWN
    }

    private enum OtherModAction {
        SOMETHING_ELSE
    }

    @Nested
    class ResolveActionId {

        @Test
        void resolveActionIdFindsAnIdHandedOverInTheFirstPosition() {
            assertThat(VanillaActionIds.resolveActionId(RowAction.MOVE_UP, null, RowAction.class))
                .isEqualTo(RowAction.MOVE_UP);
        }

        @Test
        void resolveActionIdFindsAnIdHandedOverInTheSecondPosition() {
            // The position the engine's own panel leaves the ID in for some of its widgets. A reader of
            // the first position alone drops every press from those.
            assertThat(VanillaActionIds.resolveActionId(null, RowAction.MOVE_DOWN, RowAction.class))
                .isEqualTo(RowAction.MOVE_DOWN);
        }

        @Test
        void resolveActionIdReadsTheIdOffAWidgetHandedOverInstead() {
            // What the engine's panel actually does with a button: it passes the widget and expects the
            // ID to be taken off it.
            var buttonMock = Mockito.mock(ButtonAPI.class);
            Mockito
                .when(buttonMock.getCustomData())
                .thenReturn(RowAction.MOVE_UP);

            assertThat(VanillaActionIds.resolveActionId(null, buttonMock, RowAction.class))
                .isEqualTo(RowAction.MOVE_UP);
        }

        @Test
        void resolveActionIdPrefersAnIdHandedOverToOneCarriedByAWidget() {
            // Both shapes at once, which a surface passing the ID alongside its widget produces. The
            // direct answer is taken, so no case depends on a widget being asked for data it may not hold.
            var buttonMock = Mockito.mock(ButtonAPI.class);
            Mockito
                .when(buttonMock.getCustomData())
                .thenReturn(RowAction.MOVE_DOWN);

            assertThat(VanillaActionIds.resolveActionId(RowAction.MOVE_UP, buttonMock, RowAction.class))
                .isEqualTo(RowAction.MOVE_UP);
        }

        @Test
        void resolveActionIdAnswersNothingForAnIdOfAnotherKind() {
            // A press on a widget somebody else added. Answering nothing is what leaves it alone rather
            // than acting on a guess.
            assertThat(VanillaActionIds.resolveActionId(
                    OtherModAction.SOMETHING_ELSE,
                    OtherModAction.SOMETHING_ELSE,
                    RowAction.class))
                .isNull();
        }

        @Test
        void resolveActionIdAnswersNothingForAWidgetCarryingNoId() {
            // A widget added without one, which reads the same as somebody else's: there is nothing to act
            // on either way.
            var buttonMock = Mockito.mock(ButtonAPI.class);

            assertThat(VanillaActionIds.resolveActionId(null, buttonMock, RowAction.class))
                .isNull();
        }

        @Test
        void resolveActionIdAnswersNothingWhenHandedNeitherObject() {
            assertThat(VanillaActionIds.resolveActionId(null, null, RowAction.class))
                .isNull();
        }
    }
}

package kmlib.starsector.dialogs.handoff;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins each hand-off's order: the plugin is set before it starts, and the clearing hand-off clears the dialog before
 * either.
 */
final class DialogHandOffTests {

    private InteractionDialogAPI dialogMock;
    private OptionPanelAPI optionPanelMock;
    private InteractionDialogPlugin pluginMock;

    @BeforeEach
    void setUp() {

        dialogMock = mock(InteractionDialogAPI.class);
        optionPanelMock = mock(OptionPanelAPI.class);
        pluginMock = mock(InteractionDialogPlugin.class);

        when(dialogMock.getOptionPanel())
            .thenReturn(optionPanelMock);
    }

    @Nested
    class ClearAndHandDialogTo {

        @Test
        void clearsTheDialogThenSetsAndStartsThePlugin() {

            DialogHandOff.clearAndHandDialogTo(dialogMock, pluginMock);

            var order = inOrder(optionPanelMock, dialogMock, pluginMock);

            order
                .verify(optionPanelMock)
                .clearOptions();
            order
                .verify(dialogMock)
                .hideVisualPanel();
            order
                .verify(dialogMock)
                .setPlugin(pluginMock);
            order
                .verify(pluginMock)
                .init(dialogMock);
        }
    }

    @Nested
    class HandDialogTo {

        @Test
        void setsThePluginThenStartsIt() {

            DialogHandOff.handDialogTo(dialogMock, pluginMock);

            var order = inOrder(dialogMock, pluginMock);

            order
                .verify(dialogMock)
                .setPlugin(pluginMock);
            order
                .verify(pluginMock)
                .init(dialogMock);
        }

        @Test
        void leavesTheDialogAsItStands() {

            DialogHandOff.handDialogTo(dialogMock, pluginMock);

            verify(optionPanelMock, never())
                .clearOptions();
            verify(dialogMock, never())
                .hideVisualPanel();
        }
    }
}

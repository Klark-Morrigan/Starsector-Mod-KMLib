package kmlib.starsector.ui.modal;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import kmlib.testfixtures.starsector.StubbedGlobalLogger;

import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * The screen a modal panel is stood up on, for the length of one body: the game's settings that
 * build a panel, the core UI the panel is hung from, and whether the host screen is still showing.
 *
 * <p>The first two are static reaches into a running game, which is why the plugin the panel carries
 * had nothing standing behind it: it is built inside the panel, by the settings, over the core UI, and
 * neither exists under a test JVM. Standing both in is what makes the plugin reachable at all, and it
 * hands the plugin back so its frame hooks can be driven directly.
 *
 * <p>The panel is a mock rather than a proxy because what several cases ask is what was written onto
 * it - the opacity the fade is painted as - and a stand-in that answered defaults would swallow the
 * question.
 */
final class ModalPanelScreenScope
        implements AutoCloseable {

    private final MockedStatic<Global> globalMock;
    private final MockedStatic<CoreUiOverlayPanels> overlayPanelsMock;

    // Every plugin the settings were asked to build a panel around, so a case can both drive the one
    // that was built and say how many were.
    private final List<CustomUIPanelPlugin> builtPlugins = new ArrayList<>();
    private final CustomPanelAPI panelMock = mock(CustomPanelAPI.class);

    private boolean isHostScreenShowing = true;

    private ModalPanelScreenScope(boolean isCoreUiReachable) {

        this.globalMock = StubbedGlobalLogger.openGlobalAnsweringLoggers();
        this.overlayPanelsMock = mockStatic(CoreUiOverlayPanels.class);

        var settingsMock = mock(SettingsAPI.class);

        when(settingsMock.createCustom(anyFloat(), anyFloat(), any()))
            .thenAnswer(invocation -> {
                builtPlugins.add(invocation.getArgument(2));
                return panelMock;
            });

        globalMock
            .when(Global::getSettings)
            .thenReturn(settingsMock);

        overlayPanelsMock
            .when(() -> CoreUiOverlayPanels.attachOverlayPanel(any(), any()))
            .thenReturn(isCoreUiReachable ? mock(PositionAPI.class) : null);
    }

    /**
     * Opens a scope over a screen whose core UI takes the panel - the ordinary case, and the only one
     * in which there is a plugin to drive.
     *
     * @return the open scope, to be closed by the caller's try-with-resources
     */
    static ModalPanelScreenScope openOverAReachableCoreUi() {
        return new ModalPanelScreenScope(true);
    }

    /**
     * Opens a scope over a screen whose core UI cannot be reached, so nothing can be hung from it.
     *
     * @return the open scope, to be closed by the caller's try-with-resources
     */
    static ModalPanelScreenScope openOverAnUnreachableCoreUi() {
        return new ModalPanelScreenScope(false);
    }

    @Override
    public void close() {

        // Each one closed whatever the one before did, so a scope never leaves a static stood in for
        // whatever case runs next - the failure would land there rather than here.
        try {
            globalMock.close();

        } finally {
            overlayPanelsMock.close();
        }
    }

    /**
     * @return how many panels have been built in this scope, which is how a reopen reusing the
     *         standing panel is told from one standing a second over it
     */
    int countBuiltPanels() {
        return builtPlugins.size();
    }

    /**
     * @return whether the host screen is still showing, as the panel's own read answers it
     */
    boolean isHostScreenShowing() {
        return isHostScreenShowing;
    }

    /**
     * @return the panel the modal was given, for the writes a case asks about
     */
    CustomPanelAPI resolvePanel() {
        return panelMock;
    }

    /**
     * @return the frame hooks of the panel most recently built, which is what the engine would be
     *         calling
     */
    CustomUIPanelPlugin resolvePanelPlugin() {
        return builtPlugins.get(builtPlugins.size() - 1);
    }

    /**
     * Settles whether the host screen is still on screen under the panel.
     *
     * @param isShowing whether the host-screen read answers that the screen is showing
     */
    void settleHostScreenShowing(boolean isShowing) {
        isHostScreenShowing = isShowing;
    }
}

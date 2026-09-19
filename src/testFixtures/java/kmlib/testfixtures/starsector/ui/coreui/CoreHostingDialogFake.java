package kmlib.testfixtures.starsector.ui.coreui;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;

import java.lang.reflect.Proxy;

/**
 * An interaction dialog that also stands up a core UI of its own - the game's one dialog class that
 * does both, in the shape a by-name reach meets it in: handed over as the published dialog type,
 * and answering {@code getCoreUI} from behind it. Published as a fixture variant so both KMLib's and consuming
 * mods' tests can drive a screen opened from a dialog.
 *
 * <p>A proxy rather than a fixture class, because nothing else can be both halves at once here: the
 * dialog API is far too wide to implement by hand and beyond what the mock maker will extend, while
 * the accessor the reach takes is not on that API at all. Every other call answers null, which is
 * what a caller of this gets from a dialog it never set up.
 *
 * <p>Pair it with {@link CoreUiFake} to say which state the hosted screen is in: one still on
 * screen, or one the player has closed that the dialog goes on handing out.
 */
public final class CoreHostingDialogFake {

    private CoreHostingDialogFake() {
    }

    /**
     * @param coreUi the core UI this dialog hands out
     * @return a dialog hosting it
     */
    public static InteractionDialogAPI createHosting(Object coreUi) {
        return (InteractionDialogAPI) Proxy.newProxyInstance(
            CoreHostingDialogFake.class.getClassLoader(),
            new Class<?>[] { InteractionDialogAPI.class, CoreHostingDialog.class },
            (proxy, method, arguments) ->
                "getCoreUI".equals(method.getName())
                    ? coreUi
                    : null);
    }

    /**
     * The hosting half of the contract, carried alongside the published dialog type. Public because
     * a proxy is only as visible as the least visible interface it stands on.
     */
    public interface CoreHostingDialog {
        Object getCoreUI();
    }
}

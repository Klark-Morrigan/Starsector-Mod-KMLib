package kmlib.testfixtures.starsector.intel;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * An intel manager that really holds what is added to it, so a later read sees an earlier add.
 *
 * <p>Backed by a list rather than by stubbed answers for the reason {@code StoredMemoryFake} is backed by a map: code
 * that adds an intel and then asks the manager for it - a cap checked against what was already recorded, an entry
 * installed only if absent - has to find its own write there. Two stubs that happen to agree would pass a subject that
 * reads a different class from the one it added.
 *
 * <p>Holds the rules the game's manager applies to what it keeps, and no more. An intel already held is not added
 * twice, an intel already ended is not added at all, and a read by class answers every held intel assignable to that
 * class, in the order they were added. What the game does beside holding - stamping when the player saw an intel,
 * telling the intel it was shown or removed, posting a campaign message - reaches past the manager into the running
 * game, and is a different subject; a suite about it poses it.
 */
public final class IntelManagerFake {

    private final List<IntelInfoPlugin> heldIntel = new ArrayList<>();
    private final IntelManagerAPI intelManager = buildIntelManager();

    /** The manager itself, for a caller that has to hand one over or hang it off a sector. */
    public IntelManagerAPI getIntelManager() {
        return intelManager;
    }

    /**
     * @return every held intel in the order it was added, as a copy the manager does not change under
     */
    public List<IntelInfoPlugin> listHeldIntel() {
        return List.copyOf(heldIntel);
    }

    /**
     * @param type the intel class asked about
     * @param <T>  that class
     * @return every held intel assignable to {@code type}, in the order it was added, typed for the caller that
     *         reads its fields
     */
    public <T> List<T> listHeldIntelOf(Class<T> type) {

        var matching = new ArrayList<T>();

        for (var intel : heldIntel) {
            if (type.isInstance(intel)) {
                matching.add(type.cast(intel));
            }
        }

        return matching;
    }

    /**
     * Seeds an intel as though an earlier session had added it, for a case posing a save that already holds one. It
     * goes in whatever its state, standing for what was there before the case began.
     *
     * @param intel what the save holds
     */
    public void storeIntel(IntelInfoPlugin intel) {
        heldIntel.add(intel);
    }

    // The game's rule for an add: an ended intel is refused, and one already held stays where it is.
    private Object addIntel(IntelInfoPlugin intel) {

        if (!intel.isEnded() && !heldIntel.contains(intel)) {
            heldIntel.add(intel);
        }

        return null;
    }

    // Only holding, reading and removing are posed. Every add overload lands in the one rule, the flag and the text
    // panel choosing only how the game announces the intel, which this does not do.
    private IntelManagerAPI buildIntelManager() {

        var intelManagerMock = mock(IntelManagerAPI.class);

        doAnswer(invocation -> addIntel(invocation.getArgument(0)))
            .when(intelManagerMock)
            .addIntel(any(IntelInfoPlugin.class));
        doAnswer(invocation -> addIntel(invocation.getArgument(0)))
            .when(intelManagerMock)
            .addIntel(any(IntelInfoPlugin.class), anyBoolean());
        doAnswer(invocation -> addIntel(invocation.getArgument(0)))
            .when(intelManagerMock)
            .addIntel(any(IntelInfoPlugin.class), anyBoolean(), nullable(TextPanelAPI.class));
        doAnswer(invocation -> heldIntel.remove(invocation.<IntelInfoPlugin>getArgument(0)))
            .when(intelManagerMock)
            .removeIntel(any(IntelInfoPlugin.class));

        when(intelManagerMock.getIntel())
            .thenAnswer(invocation -> listHeldIntel());
        when(intelManagerMock.getIntel(any(Class.class)))
            .thenAnswer(invocation -> listHeldIntelOf(invocation.<Class<IntelInfoPlugin>>getArgument(0)));
        when(intelManagerMock.hasIntel(any(IntelInfoPlugin.class)))
            .thenAnswer(invocation -> heldIntel.contains(invocation.<IntelInfoPlugin>getArgument(0)));
        when(intelManagerMock.hasIntelOfClass(any(Class.class)))
            .thenAnswer(invocation -> !listHeldIntelOf(invocation.<Class<?>>getArgument(0)).isEmpty());
        when(intelManagerMock.getFirstIntel(any(Class.class)))
            .thenAnswer(invocation -> readFirstIntelOf(invocation.getArgument(0)));

        return intelManagerMock;
    }

    // The first held intel of a class, or null where none is held, as the game answers.
    private IntelInfoPlugin readFirstIntelOf(Class<IntelInfoPlugin> type) {

        var matching = listHeldIntelOf(type);

        return matching.isEmpty()
            ? null
            : matching.get(0);
    }
}

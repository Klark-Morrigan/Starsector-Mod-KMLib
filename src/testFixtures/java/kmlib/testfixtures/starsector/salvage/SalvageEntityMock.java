package kmlib.testfixtures.starsector.salvage;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

/**
 * Vanilla's drop roller, {@code SalvageEntity.generateSalvage}, held still for code that delegates a roll
 * to it.
 *
 * <p>Owns the static mock's lifetime and the argument capture, so a suite stays on its own contract
 * instead of restating the captor plumbing. Install it in setup and close it in teardown - a leaked
 * static mock poisons the next class in the run to touch the same type.
 *
 * <p>Every roll answers with a fresh cargo until a case says otherwise through {@link #stubRoll}, which
 * is what a case walking the rolled stacks needs: pinning exact stacks beats chaining answers against the
 * static mock from the call site.
 *
 * <p>The capture methods pin the invocation count as well as reading the arguments, so code that rolls
 * twice where it should roll once fails the case rather than passing quietly on the first roll's
 * arguments.
 *
 * <p>Holds the seven-argument roller only, the one taking a separate random-drop multiplier. Vanilla's
 * six-argument form delegates to it, but a static mock stubs each overload on its own, so code calling
 * the shorter form gets a null cargo back and no roll recorded.
 */
public final class SalvageEntityMock implements AutoCloseable {

    private static final int FIRST_CALL = 0;
    private static final int SINGLE_CALL = 1;

    private final MockedStatic<SalvageEntity> salvageStaticMock;

    private SalvageEntityMock(MockedStatic<SalvageEntity> salvageStaticMock) {

        this.salvageStaticMock = salvageStaticMock;
    }

    /** The drop lists handed to the only roll the case expected. */
    public DropLists captureDropLists() {

        return captureDropLists(SINGLE_CALL, FIRST_CALL);
    }

    public DropLists captureDropLists(int expectedCalls) {

        return captureDropLists(expectedCalls, FIRST_CALL);
    }

    /**
     * @param expectedCalls how many rolls the case expects in total, pinned so an extra roll fails
     * @param callIndex     which of those rolls to read the lists off
     * @return the value-weighted and random drop lists that roll was given
     */
    public DropLists captureDropLists(int expectedCalls, int callIndex) {

        ArgumentCaptor<List<DropData>> valueDropsCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<List<DropData>> randomDropsCaptor = ArgumentCaptor.captor();

        salvageStaticMock.verify(
            () -> SalvageEntity.generateSalvage(
                any(),
                anyFloat(),
                anyFloat(),
                anyFloat(),
                anyFloat(),
                valueDropsCaptor.capture(),
                randomDropsCaptor.capture()),
            times(expectedCalls));

        return new DropLists(
            valueDropsCaptor.getAllValues().get(callIndex),
            randomDropsCaptor.getAllValues().get(callIndex));
    }

    /** The random source handed to the only roll the case expected, as a single-entry list. */
    public List<Random> captureRandoms() {

        return captureRandoms(SINGLE_CALL);
    }

    /**
     * Every {@code Random} threaded into the roller across {@code expectedCalls} rolls, for the
     * determinism cases that advance each one and compare the seed state between rolls.
     *
     * @param expectedCalls how many rolls the case expects in total
     * @return the random sources, in call order
     */
    public List<Random> captureRandoms(int expectedCalls) {

        ArgumentCaptor<Random> randomCaptor = ArgumentCaptor.captor();

        salvageStaticMock.verify(
            () -> SalvageEntity.generateSalvage(
                randomCaptor.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), any(), any()),
            times(expectedCalls));

        return randomCaptor.getAllValues();
    }

    /** The multipliers handed to the only roll the case expected. */
    public Scalars captureScalars() {

        return captureScalars(SINGLE_CALL, FIRST_CALL);
    }

    public Scalars captureScalars(int expectedCalls) {

        return captureScalars(expectedCalls, FIRST_CALL);
    }

    /**
     * @param expectedCalls how many rolls the case expects in total, pinned so an extra roll fails
     * @param callIndex     which of those rolls to read the multipliers off
     * @return the four multipliers that roll was given
     */
    public Scalars captureScalars(int expectedCalls, int callIndex) {

        ArgumentCaptor<Float> valueMultCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<Float> randomMultCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<Float> overallMultCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<Float> fuelMultCaptor = ArgumentCaptor.captor();

        salvageStaticMock.verify(
            () -> SalvageEntity.generateSalvage(
                any(),
                valueMultCaptor.capture(),
                randomMultCaptor.capture(),
                overallMultCaptor.capture(),
                fuelMultCaptor.capture(),
                any(),
                any()),
            times(expectedCalls));

        return new Scalars(
            valueMultCaptor.getAllValues().get(callIndex),
            randomMultCaptor.getAllValues().get(callIndex),
            overallMultCaptor.getAllValues().get(callIndex),
            fuelMultCaptor.getAllValues().get(callIndex));
    }

    /** Takes the drop roller back down. */
    @Override
    public void close() {

        salvageStaticMock.close();
    }

    /**
     * Holds the drop roller still. Every {@code generateSalvage} call answers with a fresh cargo rather
     * than reaching vanilla's roller.
     *
     * @return the installed mock, to be closed when the case is done with it
     */
    public static SalvageEntityMock install() {

        var cargoMock = mock(CargoAPI.class);
        var salvageStaticMock = mockStatic(SalvageEntity.class);

        salvageStaticMock
            .when(anyRoll())
            .thenReturn(cargoMock);

        return new SalvageEntityMock(salvageStaticMock);
    }

    /**
     * Answers every roll with {@code cargo} instead of a fresh empty one.
     *
     * @param cargo the cargo the roller hands back
     */
    public void stubRoll(CargoAPI cargo) {

        salvageStaticMock
            .when(anyRoll())
            .thenReturn(cargo);
    }

    /**
     * Pins that the roller was never reached, which is what a short circuit ahead of the roll has to
     * prove: that it drew nothing from the random source, not merely that it returned nothing.
     */
    public void verifyNoRoll() {

        salvageStaticMock.verifyNoInteractions();
    }

    // A roll with any arguments at all, for the two readings that answer every roll the same way.
    // Spelled once because the roller takes seven arguments and a matcher list that drifted between
    // the two would leave one of them answering a call the other did not.
    private static MockedStatic.Verification anyRoll() {

        return () -> SalvageEntity.generateSalvage(
            any(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), any(), any());
    }

    /**
     * The two drop lists one roll was given, in the order the roller takes them.
     *
     * @param valueDrops  the value-weighted list
     * @param randomDrops the random list
     */
    public record DropLists(
        List<DropData> valueDrops,
        List<DropData> randomDrops) {
    }

    /**
     * The four multipliers one roll was given, in the order the roller takes them.
     *
     * @param valueMult   the value-weighted multiplier
     * @param randomMult  the random-drop multiplier
     * @param overallMult the overall multiplier
     * @param fuelMult    the fuel multiplier
     */
    public record Scalars(
        float valueMult,
        float randomMult,
        float overallMult,
        float fuelMult) {
    }
}

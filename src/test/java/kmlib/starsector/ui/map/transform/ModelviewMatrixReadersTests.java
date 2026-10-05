package kmlib.starsector.ui.map.transform;

import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.CompatibilityFailures;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Covers which binding each renderer gets, and that a guarded binding is held per consumer: a
 * guarded reader records its failure against the mod it was built for, so handing one mod's reader
 * to another would tell the wrong player what they lost.
 *
 * <p>The renderer answer is handed in because no test JVM runs under Fast Rendering - the live check
 * answers "stock" there, so a suite that could not state the other answer could only ever cover the
 * stock branch.
 */
final class ModelviewMatrixReadersTests {

    // The renderer check's two answers, named so a case reads as being about which renderer is
    // underneath rather than about a bare boolean.
    private static final BooleanSupplier BRIDGE_IN_FORCE = () -> true;

    private static final BooleanSupplier STOCK_RENDERER = () -> false;

    // Two mods reading the map, so a case about each holding its own reader cannot pass on one
    // consumer standing for two.
    private static final CompatibilityConsumer MAP_OVERLAY = CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;

    private static final CompatibilityConsumer COLONY_PANEL = CompatibilityFailureFixture.COLONY_PANEL_CONSUMER;

    // A record of its own rather than the session's, so a case reads only what it recorded itself.
    private final CompatibilityFailures failures = new CompatibilityFailures();

    @Nested
    class SelectReaderForActiveRenderer {

        @Test
        void answersTheGlBindingWhereTheBridgeIsNotInForce() {

            var readers = new ModelviewMatrixReaders(STOCK_RENDERER, failures);

            assertThat(readers.selectReaderForActiveRenderer(MAP_OVERLAY))
                .isSameAs(GlModelviewMatrixReader.INSTANCE);
        }

        @Test
        void answersTheGuardedBindingWhereTheBridgeIsInForce() {

            var readers = new ModelviewMatrixReaders(BRIDGE_IN_FORCE, failures);

            assertThat(readers.selectReaderForActiveRenderer(MAP_OVERLAY))
                .isInstanceOf(FastRenderingModelviewMatrixReader.class);
        }

        @Test
        void answersEachConsumerAGuardedReaderOfItsOwn() {

            var readers = new ModelviewMatrixReaders(BRIDGE_IN_FORCE, failures);

            var mapOverlayReader = readers.selectReaderForActiveRenderer(MAP_OVERLAY);

            assertThat(readers.selectReaderForActiveRenderer(COLONY_PANEL))
                .isNotSameAs(mapOverlayReader);
        }

        @Test
        void holdsTheReaderAConsumerWasGiven() {

            // The selection a map pass asks for is asked for per frame, so a reader rebuilt per call
            // would forget its latch and ask a refusing bridge again every frame.
            var readers = new ModelviewMatrixReaders(BRIDGE_IN_FORCE, failures);

            var mapOverlayReader = readers.selectReaderForActiveRenderer(MAP_OVERLAY);

            assertThat(readers.selectReaderForActiveRenderer(MAP_OVERLAY))
                .isSameAs(mapOverlayReader);
        }

        @Test
        void recordsNothingWhileSelecting() {

            // Selecting reaches no renderer: a refused read is found where the reading is asked for.
            var readers = new ModelviewMatrixReaders(BRIDGE_IN_FORCE, failures);

            readers.selectReaderForActiveRenderer(MAP_OVERLAY);

            assertThat(failures.hasUnreported())
                .isFalse();
        }

        @Test
        void refusesASelectionMadeForNoConsumer() {

            // Refused on the stock path too, where the consumer is never read: a call that reads
            // cleanly today would otherwise fail at the one moment it must not, inside the render
            // pass that met the first refused read.
            var readers = new ModelviewMatrixReaders(STOCK_RENDERER, failures);

            assertThatNullPointerException()
                .isThrownBy(() -> readers.selectReaderForActiveRenderer(null));
        }
    }
}

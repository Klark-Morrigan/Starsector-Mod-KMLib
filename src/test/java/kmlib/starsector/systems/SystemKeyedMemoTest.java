package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.testfixtures.starsector.systems.StarSystemFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link SystemKeyedMemo#readValueFor}: that a value is worked out once per system and
 * remembered, that the memo tells apart what the key tells apart - two systems sharing an ID, a
 * system with no ID at all - and that it declines to pool the one shape the key cannot separate.
 */
final class SystemKeyedMemoTest {

    private static final String SHARED_SYSTEM_ID = "deep space";

    @Nested
    class ReadValueFor {

        @Test
        void remembersTheFirstAnswerAcrossRepeatedAsks() {
            // The point of a memo: the resolver is paid once per system, and every later ask is
            // handed the very value it produced rather than an equal one worked out again.
            var system = StarSystemFixture.buildSystem("corvus");
            var resolver = new CountingResolver();
            var memo = new SystemKeyedMemo<Object>();

            var first = memo.readValueFor(system, resolver);
            var second = memo.readValueFor(system, resolver);

            assertThat(second)
                .isSameAs(first);
            assertThat(resolver.resolvedSystems)
                .containsExactly(system);
        }

        @Test
        void keepsTwoSystemsSharingAnIdApart() {
            // The defect a memo keyed on the ID carries: a sector holds two systems under one ID,
            // so the pair is a single entry and the second system is handed the first's value. The
            // key separates them, an anchor ID being minted per system.
            var first = StarSystemFixture.buildKeyedSystem(SHARED_SYSTEM_ID, null, "8b3");
            var second = StarSystemFixture.buildKeyedSystem(SHARED_SYSTEM_ID, null, "38d53");
            var memo = new SystemKeyedMemo<Object>();

            assertThat(memo.readValueFor(second, new CountingResolver()))
                .isNotSameAs(memo.readValueFor(first, new CountingResolver()));
        }

        @Test
        void remembersASystemCarryingNoIdButAnAnchor() {
            // What the key buys over keying on the ID: an ID is only one of three arms, so a system
            // the sector never named is still remembered by the entity the engine minted for it.
            var system = StarSystemFixture.buildKeyedSystem(null, null, "8b3");
            var resolver = new CountingResolver();
            var memo = new SystemKeyedMemo<Object>();

            memo.readValueFor(system, resolver);
            memo.readValueFor(system, resolver);

            assertThat(resolver.resolvedSystems)
                .containsExactly(system);
        }

        @Test
        void resolvesASystemStatingNoArmAtAllAfreshOnEveryAsk() {
            // No ID and neither entity, so the key is blank and equals every other blank one. The
            // resolver is paid again, which is the honest price: pooling every such system under
            // the one key would hand the first one's value to the second.
            var system = StarSystemFixture.buildKeyedSystem(null, null, null);
            var resolver = new CountingResolver();
            var memo = new SystemKeyedMemo<Object>();

            memo.readValueFor(system, resolver);
            memo.readValueFor(system, resolver);

            assertThat(resolver.resolvedSystems)
                .containsExactly(system, system);
        }

        @Test
        void keepsTwoSystemsStatingNoArmAtAllApart() {
            // The conflation the blank key would cause, posed outright: two systems the sector
            // names with nothing are two systems, and each has to answer with its own value.
            var first = StarSystemFixture.buildKeyedSystem(null, null, null);
            var second = StarSystemFixture.buildKeyedSystem(null, null, null);
            var memo = new SystemKeyedMemo<Object>();

            assertThat(memo.readValueFor(second, new CountingResolver()))
                .isNotSameAs(memo.readValueFor(first, new CountingResolver()));
        }

        @Test
        void rejectsANullSystem() {
            // A null system has no key to remember it by, and what a read answers for one is that
            // read's own contract rather than the memo's.
            var memo = new SystemKeyedMemo<Object>();

            assertThatThrownBy(() -> memo.readValueFor(null, new CountingResolver()))
                .isInstanceOf(NullPointerException.class);
        }
    }

    // A resolver answering a fresh value per call and recording which system each was for, so a
    // case reads how often the memo paid for a system straight off the list.
    private static final class CountingResolver implements Function<StarSystemAPI, Object> {

        private final List<StarSystemAPI> resolvedSystems = new ArrayList<>();

        @Override
        public Object apply(StarSystemAPI system) {

            resolvedSystems.add(system);

            return new Object();
        }
    }
}

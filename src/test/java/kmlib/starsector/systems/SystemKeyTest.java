package kmlib.starsector.systems;

import kmlib.testfixtures.starsector.systems.StarSystemFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contracts of {@link SystemKey#readKeyOf} and of the key's own equality.
 *
 * <p>What is load-bearing is that two systems sharing an id are two keys. That is the live defect:
 * a modded sector holds systems whose id and name are both the same, so a case asserting an id
 * alone would pass under the very keying this type replaces.
 *
 * <p>Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree.
 */
final class SystemKeyTest {

    @Nested
    class ReadKeyOf {

        @Test
        void readsTheIdAndBothEntityIdsOffTheSystem() {

            var key = SystemKey.readKeyOf(StarSystemFixture.buildKeyedSystem("deep space", "8aa", "8b3"));

            assertThat(key.systemId())
                .isEqualTo("deep space");
            assertThat(key.centreEntityId())
                .isEqualTo("8aa");
            assertThat(key.anchorEntityId())
                .isEqualTo("8b3");
        }

        @Test
        void tellsApartTwoSystemsSharingAnId() {
            // The defect itself: vanilla's unnamed deep space systems share id and name, and are
            // separated only by the entities the engine minted for each.
            var firstKey = SystemKey.readKeyOf(StarSystemFixture.buildKeyedSystem("deep space", "8aa", "8b3"));
            var secondKey = SystemKey.readKeyOf(StarSystemFixture.buildKeyedSystem("deep space", "38d4c", "38d53"));

            assertThat(firstKey)
                .isNotEqualTo(secondKey);
        }

        @Test
        void tellsApartTwoSystemsSeparatedByTheAnchorAlone() {
            // A centre id may be a literal its creator chose, so two systems can share one. The
            // anchor is engine-minted per system, and carries the separation on its own.
            var firstKey = SystemKey.readKeyOf(
                StarSystemFixture.buildKeyedSystem("abyss", "abyss_icon_star", "4379d"));
            var secondKey = SystemKey.readKeyOf(
                StarSystemFixture.buildKeyedSystem("abyss", "abyss_icon_star", "425b5"));

            assertThat(firstKey)
                .isNotEqualTo(secondKey);
        }

        @Test
        void readsTheSameSystemAsTheSameKeyOnEveryPass() {
            // A key is what a system is looked up by later, so two reads of one system have to
            // meet - by value rather than by identity, since each pass builds its own.
            var systemMock = StarSystemFixture.buildKeyedSystem("corvus", "corvus_star", "893");

            assertThat(SystemKey.readKeyOf(systemMock))
                .isEqualTo(SystemKey.readKeyOf(systemMock));
        }

        @Test
        void keysASystemWithNeitherCentreNorAnchorByItsIdAlone() {

            var key = SystemKey.readKeyOf(StarSystemFixture.buildKeyedSystem("unknown location", null, null));

            assertThat(key.systemId())
                .isEqualTo("unknown location");
            assertThat(key.centreEntityId())
                .isEmpty();
            assertThat(key.anchorEntityId())
                .isEmpty();
        }

        @Test
        void returnsNullForANullSystem() {
            // A blank key would equal every other blank key, so nothing is answered rather than
            // something that reads as a system.
            assertThat(SystemKey.readKeyOf(null))
                .isNull();
        }
    }

    @Nested
    class Equality {

        @Test
        void treatsAnAbsentArmAndABlankArmAsTheSameArm() {
            // Both say the system offers nothing there, so a key built by hand from partial reads
            // meets one read off a system carrying no such entity.
            assertThat(new SystemKey("deep space", null, null))
                .isEqualTo(new SystemKey("deep space", "", ""));
        }

        @Test
        void tellsApartTwoKeysCarryingTheOneEntityIdOnDifferentArms() {
            // What holding the arms apart buys over composing them into one string: a system whose
            // only entity is its centre and one whose only entity is its anchor stay distinct,
            // where a composition of the same three pieces would run them together.
            assertThat(new SystemKey("deep space", "8aa", ""))
                .isNotEqualTo(new SystemKey("deep space", "", "8aa"));
        }

        @Test
        void holdsTwoSystemsSharingAnIdAsTwoEntriesOfAKeyedMap() {
            // What the collision costs today: keyed by id, the later system displaces the earlier
            // one and no pass built on that map ever sees it.
            var systemsByKey = new LinkedHashMap<SystemKey, String>();

            systemsByKey.put(
                SystemKey.readKeyOf(StarSystemFixture.buildKeyedSystem("deep space", "8aa", "8b3")),
                "first");
            systemsByKey.put(
                SystemKey.readKeyOf(StarSystemFixture.buildKeyedSystem("deep space", "38d4c", "38d53")),
                "second");

            assertThat(systemsByKey)
                .hasSize(2);
        }
    }
}

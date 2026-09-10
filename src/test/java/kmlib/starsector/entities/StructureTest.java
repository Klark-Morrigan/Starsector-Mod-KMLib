package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.CustomCampaignEntityPlugin;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.BaseCampaignObjectivePlugin;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link Structure#readHolderFactionId}, {@link Structure#readTypeName},
 * {@link Structure#isDiscoveredByPlayer}, {@link Structure#isMakeshift},
 * {@link Structure#isNonFunctional}, {@link Structure#isDisrupted} and {@link Structure#isHacked}
 * - the facts a structure answers about itself off its own entity rather than storing beside it.
 * Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree;
 * the shared builders stay on the outer class.
 */
final class StructureTest {

    @Nested
    class ReadHolderFactionId {

        @Test
        void reportsTheFactionIdTheEntityCarries() {
            // Live and unconcealed. Vanilla's map item substitutes a neutral faction here when the
            // player is elsewhere; that is a display rule, and applying it in the reading would
            // leave a caller unable to tell a genuinely neutral structure from a withheld one.
            var factionMock = mock(FactionAPI.class);

            when(factionMock.getId())
                .thenReturn("hegemony");

            var entityMock = buildStructureEntity();

            when(entityMock.getFaction())
                .thenReturn(factionMock);

            assertThat(new Structure(entityMock).readHolderFactionId())
                .isEqualTo("hegemony");
        }

        @Test
        void reportsNoHolderWhereTheEntityNamesNoFaction() {
            // Absorbed rather than refused: a holder nobody can name is compared against whatever
            // a caller compares holders for, and there is nothing here to fail on.
            assertThat(new Structure(buildStructureEntity()).readHolderFactionId())
                .isNull();
        }
    }

    @Nested
    class ReadTypeName {

        @Test
        void reportsTheSpecsDefaultName() {
            // The spec's rather than the entity's, because the entity's name is the one procgen
            // replaces - a relay named after its system no longer says anywhere what it is.
            var specMock = mock(CustomEntitySpecAPI.class);

            when(specMock.getDefaultName())
                .thenReturn("Makeshift Comm Relay");

            var entityMock = buildStructureEntity();

            when(entityMock.getCustomEntitySpec())
                .thenReturn(specMock);

            assertThat(new Structure(entityMock).readTypeName())
                .isEqualTo("Makeshift Comm Relay");
        }

        @Test
        void reportsNoTypeNameWhereTheEntityCarriesNoSpec() {

            assertThat(new Structure(buildStructureEntity()).readTypeName())
                .isNull();
        }
    }

    @Nested
    class IsDiscoveredByPlayer {

        @Test
        void reportsAStructureOnAFoundEntityAsDiscovered() {
            // An entity stops being discoverable once found, so the inclusion gate is the
            // negation of that flag rather than a reading of its own.
            assertThat(new Structure(buildStructureEntity()).isDiscoveredByPlayer())
                .isTrue();
        }

        @Test
        void reportsAStructureOnAnUndiscoveredEntityAsUndiscovered() {

            var entityMock = buildStructureEntity();

            when(entityMock.isDiscoverable())
                .thenReturn(true);

            assertThat(new Structure(entityMock).isDiscoveredByPlayer())
                .isFalse();
        }
    }

    @Nested
    class IsMakeshift {

        @Test
        void reportsATaggedVariantAsMakeshift() {
            // Off the tag rather than off the entity id, so a mod's own improvised variant reads
            // true without this having to know its id - which is the same reasoning that makes
            // the objective tag the selection rule rather than a list of six ids.
            var entityMock = buildStructureEntity();

            when(entityMock.hasTag(Tags.MAKESHIFT))
                .thenReturn(true);

            assertThat(new Structure(entityMock).isMakeshift())
                .isTrue();
        }

        @Test
        void reportsAnUntaggedStructureAsNotMakeshift() {

            assertThat(new Structure(buildStructureEntity()).isMakeshift())
                .isFalse();
        }
    }

    @Nested
    class IsNonFunctional {

        @Test
        void reportsAStructureCarryingTheFlagAsNonFunctional() {

            assertThat(new Structure(buildNonFunctionalStructureEntity()).isNonFunctional())
                .isTrue();
        }

        @Test
        void reportsAStructureWithoutTheFlagAsFunctional() {

            assertThat(new Structure(buildStructureEntity()).isNonFunctional())
                .isFalse();
        }

        @Test
        void reportsAStructureUnderAForeignPluginAsFunctional() {
            // The flag is set on this one, so the answer shows the reading standing down rather
            // than merely meeting a structure that had nothing to report. A plugin that is not
            // the objective plugin makes none of these claims, and guessing from a key it never
            // sets would be inventing the answer.
            assertThat(new Structure(buildForeignPluginStructureEntity()).isNonFunctional())
                .isFalse();
        }
    }

    @Nested
    class IsDisrupted {

        @Test
        void reportsAFactoryResetStructureAsDisrupted() {

            assertThat(new Structure(buildDisruptedStructureEntity()).isDisrupted())
                .isTrue();
        }

        @Test
        void reportsAnUntouchedStructureAsNotDisrupted() {

            assertThat(new Structure(buildStructureEntity()).isDisrupted())
                .isFalse();
        }

        @Test
        void reportsAStructureUnderAForeignPluginAsNotDisrupted() {
            // Nothing to ask: the state lives behind a plugin method this entity's plugin does not
            // have, so the read degrades rather than throwing on the first state question.
            assertThat(new Structure(buildForeignPluginStructureEntity()).isDisrupted())
                .isFalse();
        }
    }

    @Nested
    class IsHacked {

        @Test
        void reportsASniffedStructureAsHacked() {

            assertThat(new Structure(buildHackedStructureEntity()).isHacked())
                .isTrue();
        }

        @Test
        void reportsAnUntouchedStructureAsNotHacked() {

            assertThat(new Structure(buildStructureEntity()).isHacked())
                .isFalse();
        }

        @Test
        void reportsAStructureUnderAForeignPluginAsNotHacked() {

            assertThat(new Structure(buildForeignPluginStructureEntity()).isHacked())
                .isFalse();
        }
    }

    // A found, working structure nobody has touched - the state every case that is about one axis
    // poses its entity in, then stubs the one thing it is about on top.
    private static SectorEntityToken buildStructureEntity() {
        return buildStructureEntityUnder(buildObjectivePlugin(false, false), false);
    }

    // A structure with a sniffer running on it.
    private static SectorEntityToken buildHackedStructureEntity() {
        return buildStructureEntityUnder(buildObjectivePlugin(true, false), false);
    }

    // A structure knocked out by a factory reset - a lapsing state somebody did to it.
    private static SectorEntityToken buildDisruptedStructureEntity() {
        return buildStructureEntityUnder(buildObjectivePlugin(false, true), false);
    }

    // A structure built broken, or never repaired since - the standing state, on the entity's own
    // memory rather than behind a plugin method.
    private static SectorEntityToken buildNonFunctionalStructureEntity() {
        return buildStructureEntityUnder(buildObjectivePlugin(false, false), true);
    }

    // A structure carrying the objective tag but driven by somebody else's plugin, which is what
    // a mod hanging its own behaviour on the tag builds. Every state it could report is posed as
    // set, so a reading that answered off the entity instead of standing down would be visible.
    private static SectorEntityToken buildForeignPluginStructureEntity() {
        return buildStructureEntityUnder(mock(CustomCampaignEntityPlugin.class), true);
    }

    // The entity itself: found, holding for nobody, and carrying the plugin and memory the state
    // readings go through. The plugin and the memory finish their own stubbing before the
    // entity's opens, so the three do not nest into an unfinished-stubbing error.
    private static SectorEntityToken buildStructureEntityUnder(
            CustomCampaignEntityPlugin pluginMock,
            boolean isNonFunctional) {

        var memoryMock = mock(MemoryAPI.class);

        when(memoryMock.getBoolean(MemFlags.OBJECTIVE_NON_FUNCTIONAL))
            .thenReturn(isNonFunctional);

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.getCustomPlugin())
            .thenReturn(pluginMock);
        when(entityMock.getMemoryWithoutUpdate())
            .thenReturn(memoryMock);

        return entityMock;
    }

    // The plugin vanilla's structures run on, reporting the two states it owns.
    private static CustomCampaignEntityPlugin buildObjectivePlugin(
            boolean isHacked,
            boolean isReset) {

        var pluginMock = mock(BaseCampaignObjectivePlugin.class);

        when(pluginMock.isHacked())
            .thenReturn(isHacked);
        when(pluginMock.isReset())
            .thenReturn(isReset);

        return pluginMock;
    }
}

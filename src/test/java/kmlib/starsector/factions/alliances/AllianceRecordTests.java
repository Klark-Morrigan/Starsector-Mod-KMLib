package kmlib.starsector.factions.alliances;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the record's snapshot of its members: a list changed by its source after the read does not
 * reach the record, and an absent list reads as no members rather than as a fault.
 */
final class AllianceRecordTests {

    private static final String ALLIANCE_ID = "alliance-1";
    private static final String ALLIANCE_NAME = "Allied Powers";
    private static final String ASTRAL_ARMADA = "astral_armada";
    private static final String HEGEMONY = "hegemony";
    private static final String LUDDIC_CHURCH = "luddic_church";

    @Nested
    class Constructor {

        @Test
        void keepsTheMembersItWasBuiltWithWhenTheSourceListChangesLater() {
            // A source keeps its own roster live; a record handed on must not change under its
            // reader when that roster does.
            var members = new ArrayList<String>(List.of(HEGEMONY, ASTRAL_ARMADA));
            var allianceRecord = new AllianceRecord(ALLIANCE_ID, ALLIANCE_NAME, members);

            members.add(LUDDIC_CHURCH);

            assertThat(allianceRecord.membersSortedDescending())
                .containsExactly(HEGEMONY, ASTRAL_ARMADA);
        }

        @Test
        void readsAnAbsentMemberListAsNoMembers() {

            var allianceRecord = new AllianceRecord(ALLIANCE_ID, ALLIANCE_NAME, null);

            assertThat(allianceRecord.membersSortedDescending())
                .isEmpty();
        }
    }
}

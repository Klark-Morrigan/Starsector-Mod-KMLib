package kmlib.mods.console.commands;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.mods.console.commands.targets.MarketOwnerTarget;
import kmlib.mods.console.commands.targets.ResolvedTarget;
import kmlib.mods.console.commands.targets.TargetResolution;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The answer a command's target search gives when it finds what the run was aimed at.
 *
 * <p>Every command here reports what it did by naming the place and its owner, so each suite
 * needs a faction that answers for an id and a display name, wrapped in the resolution the search
 * hands back. Spelt out per suite, that arrangement is three stubs a case has to be read past
 * before the case itself starts, and two suites' worth of them drift on which fields are stubbed.
 *
 * <p>Package-private: what it builds is general enough, but the reason it exists is the pairing
 * these command suites share, and a fixture offered wider would invite suites with their own
 * faction arrangements to inherit this one's.
 */
final class CommandTargetFixture {

    private CommandTargetFixture() {
        // fixture of static builders, no instances.
    }

    /**
     * The search's answer when the run was aimed at a place somebody is named to hold.
     *
     * @param market      the place the command acts on
     * @param factionId   the id of the faction it acts for
     * @param displayName the name that faction reports
     * @return the resolution a stubbed search hands back
     */
    static TargetResolution<MarketOwnerTarget> buildResolvedTarget(
            MarketAPI market,
            String factionId,
            String displayName) {

        return new ResolvedTarget<>(
            new MarketOwnerTarget(market, buildFaction(factionId, displayName)));
    }

    // The faction the target is held by: an id and a display name, which is all any report here
    // reads off one.
    private static FactionAPI buildFaction(String factionId, String displayName) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(factionId);
        when(factionMock.getDisplayName())
            .thenReturn(displayName);

        return factionMock;
    }
}

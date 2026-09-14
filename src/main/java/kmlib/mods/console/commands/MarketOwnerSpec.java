package kmlib.mods.console.commands;

import kmlib.mods.console.commands.parsing.Parameter;
import kmlib.mods.console.commands.parsing.ParameterSpec;

import static kmlib.mods.console.commands.parsing.ParameterValues.text;

/**
 * The argument shape shared by every command that changes who holds a place: which place, then
 * which faction, both optional and in that order.
 *
 * <p>Shared rather than declared per command because it is one shape with one meaning - a player
 * who has learnt what a bare run does to one of these commands has learnt what it does to all of
 * them - and two copies of it would be free to drift on argument order or on which of the two may
 * be omitted.
 *
 * <p>Neither parameter carries a default. What an omitted argument means is a question about the
 * sector rather than about the command line - the nearest qualifying place, the player's own
 * faction - so each is left unsupplied here and answered by the search that knows how to look it
 * up.
 *
 * <p>The usage line stays the caller's, being the one part of this that is genuinely per command:
 * it names the command a player mistyped.
 */
final class MarketOwnerSpec extends ParameterSpec {

    final Parameter<String> entityId =
        acceptsPositional("entity_id", "<entity-id>", text());
    final Parameter<String> factionId =
        acceptsPositional("faction_id", "<faction-id>", text());

    MarketOwnerSpec(String usageLine) {
        super(usageLine);
    }
}

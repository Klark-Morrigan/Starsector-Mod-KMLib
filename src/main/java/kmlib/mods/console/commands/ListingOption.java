package kmlib.mods.console.commands;

/**
 * What a faction-listing run leaves off, and where it writes. Unlike the filters these compose:
 * each is answered independently, and a run may name any, all or none of them.
 *
 * <p>Together in one enum because they are one thing to the player - the words typed after the
 * filter - and are walked as one to build the usage line. Each is read by whichever part of the run
 * it belongs to: the two that shape the report by the report, and the routing one by the command
 * that decides where to put it.
 */
enum ListingOption {

    /** {@code no_holdings}: the holdings line goes, leaving one line per faction. */
    OMIT_HOLDINGS("no_holdings"),

    /** {@code no_attitude}: the faction's standing with the player goes. */
    OMIT_ATTITUDE("no_attitude"),

    /** {@code to_log}: the report goes to the game log rather than the console. */
    WRITE_TO_LOG("to_log");

    private final String keyword;

    ListingOption(String keyword) {
        this.keyword = keyword;
    }

    /** @return the bare keyword that selects this option */
    String getKeyword() {
        return keyword;
    }
}

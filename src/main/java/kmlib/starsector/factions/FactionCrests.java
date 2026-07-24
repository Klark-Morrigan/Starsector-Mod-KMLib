package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;

import kmlib.text.KmlibStrings;

/**
 * Reads a faction's crest into the sprite path a UI row draws, confining the {@code FactionAPI} crest
 * lookup the KM mods share to one place and treating a blank crest the same as an absent one.
 *
 * <p>Sibling to {@link StarsectorFactionColors}, which confines the faction colour lookup: this reads
 * the faction's crest. A faction with no authored crest - a null faction, or a null or blank crest
 * string - resolves to a null path, so every caller agrees on when a crest is present rather than
 * each re-deciding what an empty crest string means.
 */
public final class FactionCrests {

    private FactionCrests() {
    }

    /**
     * Resolves a faction's crest sprite path, or null when the faction is absent or carries no
     * authored crest. A blank crest string is treated as no crest, and a surrounding-whitespace path
     * is trimmed, so the returned path is either a non-blank sprite path or null.
     *
     * @param faction the faction whose crest is read, or null
     * @return the trimmed crest sprite path, or null when absent
     */
    public static String resolveCrestPath(FactionAPI faction) {
        if (faction == null) {
            return null;
        }
        var crest = faction.getCrest();
        return KmlibStrings.hasText(crest) ? crest.trim() : null;
    }
}

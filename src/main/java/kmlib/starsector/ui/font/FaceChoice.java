package kmlib.starsector.ui.font;

import kmlib.settings.LabeledChoice;
import kmlib.settings.LabeledChoices;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * What a player picked for the face a category of text draws in: {@link AutoFace}, leaving the pick to
 * {@link FaceResolver}, or {@link NamedFace}, one atlas by name.
 *
 * <p>A Radio's stored value, so it is a {@link LabeledChoice}: LunaLib stores the label of the option
 * picked, and a named face's label is its atlas's basename. Basenames are identifiers rather than
 * captions, so the options are spelt the same in every locale and a choice survives a locale switch.
 *
 * <p>Sealed over the two because each answers something the other cannot: only a named face has a face,
 * and only the automatic one leaves the answer to what the install holds. {@link #selectByCase} is the
 * fold a caller states both cases through.
 */
public sealed interface FaceChoice
        extends LabeledChoice
        permits FaceChoice.AutoFace, FaceChoice.NamedFace {

    /** The automatic choice, which every category offers first and a stored value falls back to. */
    AutoFace AUTO_FACE = new AutoFace();

    /**
     * The choice a stored Radio label names: the face whose basename it is, or {@link #AUTO_FACE} for the
     * automatic label, a label naming no face this library knows, and nothing stored at all. Falling back
     * to the automatic choice rather than to one face is what keeps a stale label from pinning a category
     * to an atlas the player never picked.
     *
     * @param label the stored label, may be {@code null}
     * @return the choice that label names
     */
    static FaceChoice fromLabel(String label) {
        return LabeledChoices.fromLabel(listEveryChoice(), label, AUTO_FACE);
    }

    /**
     * The labels a Radio choosing a face lists, in order: the automatic choice, then every face the enum
     * names by basename. The one statement of the options, the same for every category, so a settings
     * table can be held to it.
     *
     * @return the Radio's option labels
     */
    static List<String> listChoiceLabels() {
        return Arrays.stream(listEveryChoice())
            .map(FaceChoice::getLabel)
            .toList();
    }

    /**
     * Folds this choice by case, so a caller states what each one means or does not compile.
     *
     * @param autoCase  what the automatic choice comes to
     * @param namedCase what a named face comes to, given that face
     * @param <R>       what the fold answers
     * @return the answer for this choice's case
     */
    <R> R selectByCase(Supplier<R> autoCase, Function<StarsectorFont, R> namedCase);

    // Every choice there is: the automatic one, then each face by name. Built per lookup, a stored label
    // being read once per settings read rather than per frame.
    private static FaceChoice[] listEveryChoice() {

        var fonts = StarsectorFont.values();
        var choices = new FaceChoice[fonts.length + 1];

        choices[0] = AUTO_FACE;

        for (var index = 0; index < fonts.length; index++) {
            choices[index + 1] = new NamedFace(fonts[index]);
        }
        return choices;
    }

    /**
     * The automatic choice: the category's preferred face unless the text it draws needs another.
     */
    record AutoFace() implements FaceChoice {

        // The stored value of the automatic option. A stored key like any Radio label, so it is frozen
        // once shipped and never translated.
        private static final String AUTO_LABEL = "Auto";

        @Override
        public String getLabel() {
            return AUTO_LABEL;
        }

        @Override
        public <R> R selectByCase(Supplier<R> autoCase, Function<StarsectorFont, R> namedCase) {
            return autoCase.get();
        }
    }

    /**
     * One atlas, by name.
     *
     * @param font the face picked
     */
    record NamedFace(
        StarsectorFont font) implements FaceChoice {

        @Override
        public String getLabel() {
            return font.getBasename();
        }

        @Override
        public <R> R selectByCase(Supplier<R> autoCase, Function<StarsectorFont, R> namedCase) {
            return namedCase.apply(font);
        }
    }
}

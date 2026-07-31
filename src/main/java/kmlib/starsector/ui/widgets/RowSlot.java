package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.controls.TriangleDirection;
import kmlib.starsector.ui.text.StyledSpanMeasurer;
import kmlib.starsector.ui.text.TextSpan;

import java.util.Objects;

/**
 * What a row carries beside its label - a small image, a run of text, a tick, a direction triangle, or
 * nothing. The two flanking columns of a row hold none of these exclusively: the trailing one holds a
 * value in one stack of rows and a sort marker in the next, and the leading one a crest here and a tick
 * box there. Modelled as one field per kind, a row grows a field for every kind that ever arrives and
 * carries all but one of them unset on every row that is built.
 *
 * <p>A sealed set instead: a new kind of flanking element is a new member, and every layout that
 * branches on the kind stops compiling until it handles the new one - which is the whole difference
 * between a model that can absorb a new kind and one that quietly draws nothing for it.
 *
 * <p>Every member answers its own width for a line of a given height, because that width is the one
 * fact a stack of rows needs to reserve a column all of them share, and the arithmetic behind it
 * differs per kind: an image squares off the line, a triangle takes a fraction of it, and a run of text
 * is as wide as its glyphs measure. What a slot does <em>not</em> answer is the padding around it or the
 * gap to the label - those belong to whatever lays out the columns, since they are facts about a stack
 * of rows rather than about anything a row holds.
 *
 * <p>Slots are visual only. A tick is clickable where an image is not, but which cell was hit and what
 * a click does stay with {@link kmlib.starsector.ui.controls.ControlSpec} and the container that owns
 * the row's footprint - the same split that keeps {@code ControlAction} off the control's own content.
 */
public sealed interface RowSlot {

    /**
     * What a slot with nothing to draw is charged. Named here rather than per member so that an
     * unfilled slot and a run that came out blank cannot be charged two different nothings, and so a
     * container comparing a measured width against it reads the intent rather than a bare zero.
     */
    float NO_WIDTH = 0f;

    /**
     * The slot a row leaves unfilled. Shared rather than built per row: an empty slot holds no state
     * that could tell two of them apart, and one spelling of "nothing here" is what stops a null slot
     * and an empty one both meaning the same thing in different places.
     */
    RowSlot EMPTY = new Empty();

    /**
     * How wide this slot draws on a line {@code lineHeight} tall, before any padding or gap the columns
     * around it add. Sized off the line height rather than a fixed step so that a stack of rows drawn at
     * one size shows even slots, and a stack drawn larger scales with its text.
     *
     * @param lineHeight   the height of the line the slot sits on, in UI units
     * @param spanMeasurer the width measurement already bound to the look that line draws in, spent
     *                     only by a slot whose width depends on its glyphs
     * @return the slot's width, in UI units, and nothing at all for a slot with nothing to draw
     */
    float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer);

    /**
     * A small image drawn in the slot - a faction crest, an icon, whatever the host supplies. It hangs
     * as a square as tall as its line, so it sits level with the label beside it whatever face that
     * label draws in, and a stack of rows shows equally-sized images without any row stating a size.
     *
     * @param spritePath the image's {@code graphics} texture path
     */
    record Image(
        String spritePath) implements RowSlot {

        /**
         * Rejects a null path, since a slot holding no image is {@link #EMPTY} rather than an image
         * with nothing to load. A null otherwise surfaces at the texture lookup inside a draw call,
         * well past the point that could say which row was meant.
         */
        public Image {
            Objects.requireNonNull(spritePath, "spritePath");
        }

        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            return lineHeight;
        }
    }

    /**
     * A run of text drawn in the slot - a value, a count, a short caption. It carries its colour with
     * it as a {@link TextSpan} does, so a slot's text and the colour it draws in cannot be reached for
     * separately and drift.
     *
     * @param textSpan the run and the colour it draws in, before any opacity fade the host applies
     */
    record Text(
        TextSpan textSpan) implements RowSlot {

        /**
         * Rejects a null span, since a slot holding no text is {@link #EMPTY} - and a blank-but-present
         * span is how a run that draws nothing is spelled, so a null is a mistake rather than an
         * unstated absence.
         */
        public Text {
            Objects.requireNonNull(textSpan, "textSpan");
        }

        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            // A run that came out blank reserves nothing, so a caller that assembled one from parts and
            // came up empty gets the column it would have had without it rather than a gap held open in
            // front of no glyphs. Which runs read as blank is TextSpan's rule, not a second one here.
            if (!textSpan.hasText()) {
                return NO_WIDTH;
            }
            return (float) spanMeasurer.measureSpanWidth(textSpan);
        }
    }

    /**
     * A tick box drawn in the slot, ticked or clear. It squares off its line as an image does, so a row
     * that leads with a tick lays its label exactly where a crested row lays its own. The lit state
     * rides along because it is what the box draws as, not what a click on it means - whether the row
     * is clickable at all, and what the click does, stay with the container.
     *
     * @param isTicked whether the box draws ticked
     */
    record Tick(
        boolean isTicked) implements RowSlot {

        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            return lineHeight;
        }
    }

    /**
     * A small filled triangle drawn in the slot, pointing up or down - the marker a sort selector shows
     * where the body font renders no up/down glyph. What the direction stands for is the host's, so the
     * slot names the shape and nothing more.
     *
     * @param triangleDirection which way the triangle points
     */
    record Triangle(
        TriangleDirection triangleDirection) implements RowSlot {

        /**
         * Rejects a null direction, since a slot showing no triangle is {@link #EMPTY} rather than a
         * triangle pointing nowhere - and a renderer given one has no shape to fall back on.
         */
        public Triangle {
            Objects.requireNonNull(triangleDirection, "triangleDirection");
        }

        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            // The triangle's slot is sized where its box is, so the width reserved for the marker and
            // the width it is drawn at cannot disagree.
            return IconLabelRow.computeDirectionTriangleSlotWidth(lineHeight);
        }
    }

    /**
     * A slot a row does not fill. It is a member of the set rather than a null so that measuring and
     * placing a row take one path whether or not each of its columns is filled, and so a row that fills
     * neither flanking column is still a row of the same shape as one that fills both.
     */
    record Empty() implements RowSlot {

        // The column an unfilled slot sits in may still be reserved by another row that fills it, which
        // is the container's decision rather than this slot's - this one is worth nothing either way.
        @Override
        public float computeWidth(float lineHeight, StyledSpanMeasurer spanMeasurer) {
            return NO_WIDTH;
        }
    }
}

package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins {@link UniformGrid}: an item is filed under every cell of side 10 its point, segment box
 * or bounds touch, with floor indexing so the cells either side of zero stay apart; a nearby
 * query reads every cell the square of its radius touches and answers each item once in filing
 * order; candidate pairs are the distinct items sharing a cell, each pair once, earlier-filed
 * first; and the adaptive side divides the longer extent by the square root of the item count.
 */
final class UniformGridTests {

    private static final double CELL_SIDE = 10;

    @Nested
    class Constructor {

        @Test
        void refusesAZeroCellSide() {
            assertThatThrownBy(() -> new UniformGrid<String>(0))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void refusesANegativeCellSide() {
            assertThatThrownBy(() -> new UniformGrid<String>(-5))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void refusesACellSideThatIsNotANumber() {
            assertThatThrownBy(() -> new UniformGrid<String>(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class FileBounds {

        @Test
        void filesUnderEveryCellTheBoxTouches() {
            // [5,15]x[5,15] reaches into the four cells round (10, 10).
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.fileBounds(new Bounds(5, 5, 15, 15), "box");

            assertThat(grid.listItemsNear(15, 15, 0))
                .containsExactly("box");
        }

        @Test
        void leavesOutACellTheBoxDoesNotReach() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.fileBounds(new Bounds(5, 5, 15, 15), "box");

            assertThat(grid.listItemsNear(25, 5, 0))
                .isEmpty();
        }

        @Test
        void filesABoxEndingOnACellEdgeInTheCellBeyond() {
            // The box ends at x = 10, which is where the next cell begins.
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.fileBounds(new Bounds(0, 0, 10, 5), "box");

            assertThat(grid.listItemsNear(15, 5, 0))
                .containsExactly("box");
        }
    }

    @Nested
    class FilePoint {

        @Test
        void filesUnderTheCellThePointFallsIn() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.filePoint(5, 5, "a");

            assertThat(grid.listItemsNear(1, 9, 0))
                .containsExactly("a");
        }

        @Test
        void keepsTheCellsEitherSideOfZeroApart() {
            // (-5, -5) is in cell (-1, -1), not the cell (0, 0) truncation would put it in.
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.filePoint(-5, -5, "a");

            assertThat(grid.listItemsNear(1, 1, 0))
                .isEmpty();
        }
    }

    @Nested
    class FileSegment {

        @Test
        void filesUnderEveryCellAlongTheSegment() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.fileSegment(new double[] {5, 5}, new double[] {25, 5}, "s");

            assertThat(grid.listItemsNear(15, 5, 0))
                .containsExactly("s");
        }

        @Test
        void filesTheSameWayWhicheverEndComesFirst() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.fileSegment(new double[] {25, 5}, new double[] {5, 5}, "s");

            assertThat(grid.listItemsNear(5, 5, 0))
                .containsExactly("s");
        }

        @Test
        void filesADiagonalUnderCellsItOnlyPassesBeside() {
            // From (5, 5) to (15, 15) the segment never enters the cell at [10,20]x[0,10], but its
            // bounding box does.
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.fileSegment(new double[] {5, 5}, new double[] {15, 15}, "s");

            assertThat(grid.listItemsNear(15, 5, 0))
                .containsExactly("s");
        }

        @Test
        void leavesOutACellPastTheSegmentsEnd() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.fileSegment(new double[] {5, 5}, new double[] {25, 5}, "s");

            assertThat(grid.listItemsNear(35, 5, 0))
                .isEmpty();
        }
    }

    @Nested
    class ListCandidatePairs {

        @Test
        void pairsItemsSharingACell() {
            // "c" is two cells away from the other two.
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.filePoint(5, 5, "a");
            grid.filePoint(6, 6, "b");
            grid.filePoint(25, 5, "c");

            assertThat(grid.listCandidatePairs())
                .containsExactly(new UniformGrid.ItemPair<>("a", "b"));
        }

        @Test
        void answersAPairSharingSeveralCellsOnce() {
            // Two parallel segments a unit apart share all three cells they cross.
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.fileSegment(new double[] {5, 5}, new double[] {25, 5}, "one");
            grid.fileSegment(new double[] {5, 6}, new double[] {25, 6}, "other");

            assertThat(grid.listCandidatePairs())
                .containsExactly(new UniformGrid.ItemPair<>("one", "other"));
        }

        @Test
        void ordersPairsByFilingOrder() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.filePoint(5, 5, "c");
            grid.filePoint(6, 6, "a");
            grid.filePoint(7, 7, "b");

            assertThat(grid.listCandidatePairs())
                .containsExactly(
                    new UniformGrid.ItemPair<>("c", "a"),
                    new UniformGrid.ItemPair<>("c", "b"),
                    new UniformGrid.ItemPair<>("a", "b"));
        }

        @Test
        void putsTheEarlierFiledItemFirstWhateverOrderACellHoldsThem() {
            // "b" reaches the shared cell (1, 0) before "a" does, but "a" was filed first.
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.filePoint(5, 5, "a");
            grid.filePoint(15, 5, "b");
            grid.filePoint(15, 5, "a");

            assertThat(grid.listCandidatePairs())
                .containsExactly(new UniformGrid.ItemPair<>("a", "b"));
        }

        @Test
        void neverPairsAnItemWithItself() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.filePoint(5, 5, "a");
            grid.filePoint(6, 6, "a");

            assertThat(grid.listCandidatePairs())
                .isEmpty();
        }

        @Test
        void isEmptyForAnEmptyGrid() {
            assertThat(new UniformGrid<String>(CELL_SIDE).listCandidatePairs())
                .isEmpty();
        }
    }

    @Nested
    class ListItemsNear {

        @Test
        void readsOnlyThePointsOwnCellAtRadiusZero() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.filePoint(5, 5, "a");
            grid.filePoint(15, 5, "b");

            assertThat(grid.listItemsNear(5, 5, 0))
                .containsExactly("a");
        }

        @Test
        void readsEveryCellTheRadiusSquareTouches() {
            // A radius of 20 round (5, 5) reaches from x = -15 to x = 25: cells -2 to 2.
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.filePoint(5, 5, "a");
            grid.filePoint(25, 5, "b");
            grid.filePoint(35, 5, "c");

            assertThat(grid.listItemsNear(5, 5, 20))
                .containsExactly("a", "b");
        }

        @Test
        void answersInFilingOrder() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.filePoint(15, 5, "b");
            grid.filePoint(5, 5, "a");

            assertThat(grid.listItemsNear(10, 5, 10))
                .containsExactly("b", "a");
        }

        @Test
        void answersAnItemFiledInSeveralTouchedCellsOnce() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.fileSegment(new double[] {5, 5}, new double[] {25, 5}, "s");
            grid.filePoint(15, 15, "s");

            assertThat(grid.listItemsNear(15, 5, 10))
                .containsExactly("s");
        }

        @Test
        void isEmptyWhereNothingIsFiled() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            grid.filePoint(5, 5, "a");

            assertThat(grid.listItemsNear(105, 105, 10))
                .isEmpty();
        }

        @Test
        void refusesANegativeRadius() {
            var grid = new UniformGrid<String>(CELL_SIDE);

            assertThatThrownBy(() -> grid.listItemsNear(5, 5, -1))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class MeasureAdaptiveCellSide {

        @Test
        void dividesTheLongerSideByTheRootOfTheItemCount() {
            // 100 wide, 40 tall, four items: 100 / 2.
            assertThat(UniformGrid.measureAdaptiveCellSide(new Bounds(0, 0, 100, 40), 4))
                .isEqualTo(50.0);
        }

        @Test
        void dividesTheHeightWhenTheBoxIsTaller() {
            // 30 wide, 60 tall, nine items: 60 / 3.
            assertThat(UniformGrid.measureAdaptiveCellSide(new Bounds(0, 0, 30, 60), 9))
                .isEqualTo(20.0);
        }

        @Test
        void neverFallsBelowTheShortestEdgeForASetAtOnePoint() {
            assertThat(UniformGrid.measureAdaptiveCellSide(new Bounds(3, 3, 3, 3), 9))
                .isEqualTo(1e-6);
        }

        @Test
        void makesTheWholeExtentOneCellForNoItems() {
            assertThat(UniformGrid.measureAdaptiveCellSide(new Bounds(0, 0, 30, 60), 0))
                .isEqualTo(60.0);
        }
    }
}

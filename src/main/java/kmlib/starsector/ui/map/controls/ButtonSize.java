package kmlib.starsector.ui.map.controls;

/**
 * How big a control standing on a map's filter row has to be.
 *
 * <p>The two travel together because they are one measurement taken off one row, and a holder of
 * them apart could pair a width read from a row with a height read from the row that replaced it.
 * That is also why they cross the seam between measuring a row and writing into it as one value:
 * splitting them at the call would put back exactly the mistake keeping them together prevents.
 */
record ButtonSize(
    float width,
    float height) {
}

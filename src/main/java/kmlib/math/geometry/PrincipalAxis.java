package kmlib.math.geometry;

import java.util.List;

/**
 * The dominant direction a cloud of 2D points spreads along, and how far it
 * spreads there - the result of fitting a principal axis to the points.
 *
 * <p>{@code (centroidX, centroidY)} is the point cloud's mean position, the natural
 * anchor to hang a label or marker on. {@code (axisX, axisY)} is a unit vector along
 * the direction of greatest variance (the first principal component), so a label laid
 * along it runs down the cloud's long dimension. {@code length} is the extent of the
 * points projected onto that axis - the span from the rearmost to the foremost point -
 * so it estimates how long a label the cloud can carry. {@code minorLength} is the
 * matching extent along the perpendicular (second principal) direction - how wide the
 * cloud is across its axis - so the two together say how strung-out its shape is.
 *
 * <p>The axis is a direction, not an arrow: its sign is arbitrary (a cloud has no
 * inherent front or back), so a consumer that needs upright text may flip it freely.
 */
public record PrincipalAxis(double centroidX, double centroidY, double axisX, double axisY,
        double length, double minorLength) {

    /**
     * Fits a {@link PrincipalAxis} to a cloud of {@code {x, y}} points: the mean
     * position, the unit direction the points spread along most, and their extent
     * along it - the first principal component of the cloud.
     *
     * <p>Found in closed form from the 2x2 covariance matrix of the points about
     * their mean: its larger eigenvalue's eigenvector is the axis, and projecting
     * the points onto that axis gives the span. A single point (or coincident
     * points) has no direction of spread, and a perfectly round cloud no preferred
     * one, so both fall back to the x-axis with zero length.
     *
     * @param points the point cloud, each a {@code {x, y}} pair; must be non-empty
     * @return the fitted centroid, unit axis, and projected length
     * @throws IllegalArgumentException if {@code points} is empty (a mean and a
     *         spread are undefined with nothing to average)
     */
    public static PrincipalAxis fitTo(List<double[]> points) {
        if (points.isEmpty()) {
            throw new IllegalArgumentException("Cannot fit a principal axis to no points");
        }
        var count = points.size();
        var mean = Points.computeMean(points);
        var centroidX = mean[0];
        var centroidY = mean[1];

        // Covariance of the points about their mean: varX/varY are the spread along
        // each axis and covXY how the two co-vary. The eigenvectors of this matrix
        // are the cloud's principal directions.
        var varX = 0.0;
        var varY = 0.0;
        var covXY = 0.0;
        for (var point : points) {
            var deltaX = point[0] - centroidX;
            var deltaY = point[1] - centroidY;
            varX += deltaX * deltaX;
            varY += deltaY * deltaY;
            covXY += deltaX * deltaY;
        }
        varX /= count;
        varY /= count;
        covXY /= count;

        var axis = computeMajorEigenvector(varX, varY, covXY);
        var length = computeProjectedExtent(points, axis[0], axis[1]);
        // The minor extent is the same projection along the perpendicular direction
        // (-axisY, axisX), so length and minorLength read the cloud's two dimensions
        // in the same units and a caller can compare them for its shape.
        var minorLength = computeProjectedExtent(points, -axis[1], axis[0]);
        return new PrincipalAxis(centroidX, centroidY, axis[0], axis[1], length, minorLength);
    }

    /**
     * How strung-out the cloud is along its axis, {@code 0} (round - it spreads about
     * equally every way, so no direction is meaningful) to nearly {@code 1} (a thin
     * line). Read as {@code 1 - minorLength / length}: how far the across-axis width
     * falls short of the along-axis length. A cloud with no along-axis spread at all
     * (a single point, coincident points) has no shape to speak of and reads {@code 0},
     * so a consumer can treat a low value as "this axis direction is not trustworthy".
     *
     * @return the elongation in {@code [0, 1)}
     */
    public double computeElongation() {
        if (length < Limits.MIN_EDGE_LENGTH) {
            return 0.0;
        }
        return 1.0 - Math.min(minorLength, length) / length;
    }

    // The unit eigenvector of the symmetric covariance matrix [[varX, covXY],
    // [covXY, varY]] for its larger eigenvalue - the direction of greatest spread.
    // Two candidate eigenvectors span the eigenspace; the one with the larger norm
    // is the numerically stable pick (the other collapses toward zero on an axis-
    // aligned cloud). Both collapse only when the cloud has no dominant direction
    // (a point, coincident points, or an isotropic spread), where the x-axis is an
    // arbitrary but valid choice.
    private static double[] computeMajorEigenvector(double varX, double varY, double covXY) {
        // Eigenvalues of the 2x2 solve to halfTrace +/- sqrt(halfTrace^2 - det);
        // the larger takes the plus. Framed around the half-trace so the radical
        // needs no 4 * det term.
        var halfTrace = (varX + varY) / 2.0;
        var determinant = varX * varY - covXY * covXY;
        var majorEigenvalue = halfTrace + Math.sqrt(Math.max(0.0, halfTrace * halfTrace - determinant));
        var candidateX = majorEigenvalue - varY;
        var candidateY = covXY;
        var alternateX = covXY;
        var alternateY = majorEigenvalue - varX;
        if (Points.computeVectorLength(alternateX, alternateY)
                > Points.computeVectorLength(candidateX, candidateY)) {
            candidateX = alternateX;
            candidateY = alternateY;
        }
        var axis = Points.computeUnitVector(candidateX, candidateY, Limits.MIN_AXIS_VECTOR_LENGTH);
        return axis != null ? axis : new double[] {1.0, 0.0};
    }

    // The span of the points projected onto the unit axis: the foremost projection
    // minus the rearmost. The centroid offset cancels in the difference, so the extent
    // reads straight off the points' own projections. Zero for a single point.
    private static double computeProjectedExtent(List<double[]> points, double axisX,
            double axisY) {
        var extent = Points.projectExtentOnto(points, axisX, axisY);
        return extent[1] - extent[0];
    }
}

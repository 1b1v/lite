package com.almarel.orion.curve;

import com.almarel.orion.curve.data.CurvePoint;
import com.google.inject.Singleton;
import com.sk89q.worldedit.math.Vector3;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class CurvePathService {

    private static final double BLOCK_CENTER_OFFSET = 0.5;
    private static final int STEPS_PER_BLOCK = 20;
    private static final int MIN_STEPS = 4;
    private static final double EPSILON = 1e-6;
    private static final double SMOOTH_SAG_FACTOR = 0.999;

    public List<CurvePoint> calculatePath(List<Vector3> rawPoints, double sag) {
        if (rawPoints.size() == 2) {
            return calculateTwoPointPath(rawPoints, sag);
        } else {
            return calculateCatmullRomPath(rawPoints, sag);
        }
    }

    private List<CurvePoint> calculateTwoPointPath(List<Vector3> rawPoints, double sag) {
        Vector3 start = centerPoint(rawPoints.get(0));
        Vector3 end = centerPoint(rawPoints.get(1));
        Vector3 delta = end.subtract(start);

        double smoothSag = sag * SMOOTH_SAG_FACTOR;
        Vector3 cp1 = start.add(delta.multiply(1.0 / 3.0)).add(0, smoothSag, 0);
        Vector3 cp2 = start.add(delta.multiply(2.0 / 3.0)).add(0, smoothSag, 0);

        int steps = calculateSteps(delta.length());
        List<CurvePoint> path = new ArrayList<>(steps + 1);

        sampleBezierSegment(start, cp1, cp2, end, steps, path);
        return path;
    }

    private void sampleBezierSegment(Vector3 start,
                                     Vector3 cp1,
                                     Vector3 cp2,
                                     Vector3 end,
                                     int steps,
                                     List<CurvePoint> path) {
        double sx = start.getX(), sy = start.getY(), sz = start.getZ();
        double cp1x = cp1.getX(), cp1y = cp1.getY(), cp1z = cp1.getZ();
        double cp2x = cp2.getX(), cp2y = cp2.getY(), cp2z = cp2.getZ();
        double ex = end.getX(), ey = end.getY(), ez = end.getZ();

        double lastX = sx, lastY = sy, lastZ = sz;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double mt = 1.0 - t;
            double mt2 = mt * mt;
            double t2 = t * t;

            double c0 = mt2 * mt;
            double c1 = 3.0 * mt2 * t;
            double c2 = 3.0 * mt * t2;
            double c3 = t2 * t;

            double px = c0 * sx + c1 * cp1x + c2 * cp2x + c3 * ex;
            double py = c0 * sy + c1 * cp1y + c2 * cp2y + c3 * ey;
            double pz = c0 * sz + c1 * cp1z + c2 * cp2z + c3 * ez;

            double tx = (i == 0) ? (cp1x - sx) : (px - lastX);
            double ty = (i == 0) ? (cp1y - sy) : (py - lastY);
            double tz = (i == 0) ? (cp1z - sz) : (pz - lastZ);

            path.add(createCurvePoint(px, py, pz, tx, ty, tz));
            lastX = px;
            lastY = py;
            lastZ = pz;
        }
    }

    private List<CurvePoint> calculateCatmullRomPath(List<Vector3> rawPoints, double sag) {
        int segmentCount = rawPoints.size() - 1;
        List<Vector3> pts = buildPaddedPoints(rawPoints);
        double[] segmentLengths = calculateSegmentLengths(pts, segmentCount);
        double totalLength = calculateTotalLength(segmentLengths);
        int totalEstimatedSteps = estimateTotalSteps(segmentLengths);

        List<CurvePoint> path = new ArrayList<>(totalEstimatedSteps);
        double currentBaseLength = 0;

        for (int i = 0; i < segmentCount; i++) {
            double segDist = segmentLengths[i];
            sampleCatmullRomSegment(pts, i, segDist, currentBaseLength, totalLength, sag, path);
            currentBaseLength += segDist;
        }

        return path;
    }

    private void sampleCatmullRomSegment(List<Vector3> pts,
                                         int index,
                                         double segDist,
                                         double currentBaseLength,
                                         double totalLength,
                                         double sag,
                                         List<CurvePoint> path) {
        Vector3 vP0 = pts.get(index);
        Vector3 vP1 = pts.get(index + 1);
        Vector3 vP2 = pts.get(index + 2);
        Vector3 vP3 = pts.get(index + 3);

        double p1x = vP1.getX(), p1y = vP1.getY(), p1z = vP1.getZ();
        double p2x = vP2.getX(), p2y = vP2.getY(), p2z = vP2.getZ();

        double m1x = (p2x - vP0.getX()) * 0.5;
        double m1y = (p2y - vP0.getY()) * 0.5;
        double m1z = (p2z - vP0.getZ()) * 0.5;

        double m2x = (vP3.getX() - p1x) * 0.5;
        double m2y = (vP3.getY() - p1y) * 0.5;
        double m2z = (vP3.getZ() - p1z) * 0.5;

        int steps = calculateSteps(segDist);
        boolean applySag = sag != 0.0 && totalLength > EPSILON;

        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            double t2 = t * t;
            double t3 = t2 * t;

            double h00 = 2.0 * t3 - 3.0 * t2 + 1.0;
            double h10 = t3 - 2.0 * t2 + t;
            double h01 = -2.0 * t3 + 3.0 * t2;
            double h11 = t3 - t2;

            double px = h00 * p1x + h10 * m1x + h01 * p2x + h11 * m2x;
            double py = h00 * p1y + h10 * m1y + h01 * p2y + h11 * m2y;
            double pz = h00 * p1z + h10 * m1z + h01 * p2z + h11 * m2z;

            double dh00 = 6.0 * t2 - 6.0 * t;
            double dh10 = 3.0 * t2 - 4.0 * t + 1.0;
            double dh01 = -6.0 * t2 + 6.0 * t;
            double dh11 = 3.0 * t2 - 2.0 * t;

            double tx = dh00 * p1x + dh10 * m1x + dh01 * p2x + dh11 * m2x;
            double ty = dh00 * p1y + dh10 * m1y + dh01 * p2y + dh11 * m2y;
            double tz = dh00 * p1z + dh10 * m1z + dh01 * p2z + dh11 * m2z;

            if (applySag) {
                double globalT = (currentBaseLength + (segDist * t)) / totalLength;
                py += calculateSagOffset(sag, globalT);
                ty += calculateSagTangentOffset(sag, globalT, totalLength);
            }

            path.add(createCurvePoint(px, py, pz, tx, ty, tz));
        }
    }

    private double calculateSagOffset(double sag, double globalT) {
        return sag * 4.0 * globalT * (1.0 - globalT);
    }

    private double calculateSagTangentOffset(double sag, double globalT, double totalLength) {
        return sag * 4.0 * (1.0 - 2.0 * globalT) / totalLength;
    }

    private List<Vector3> buildPaddedPoints(List<Vector3> rawPoints) {
        int numPoints = rawPoints.size();
        List<Vector3> pts = new ArrayList<>(numPoints + 2);

        pts.add(centerPoint(rawPoints.get(0)));
        for (Vector3 p : rawPoints) {
            pts.add(centerPoint(p));
        }
        pts.add(centerPoint(rawPoints.get(numPoints - 1)));

        return pts;
    }

    private double[] calculateSegmentLengths(List<Vector3> pts, int segmentCount) {
        double[] lengths = new double[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            lengths[i] = pts.get(i + 2).distance(pts.get(i + 1));
        }
        return lengths;
    }

    private double calculateTotalLength(double[] segmentLengths) {
        double total = 0;
        for (double len : segmentLengths) {
            total += len;
        }
        return total;
    }

    private int estimateTotalSteps(double[] segmentLengths) {
        int total = 0;
        for (double len : segmentLengths) {
            total += calculateSteps(len) + 1;
        }
        return total;
    }

    private Vector3 centerPoint(Vector3 raw) {
        return raw.add(BLOCK_CENTER_OFFSET, BLOCK_CENTER_OFFSET, BLOCK_CENTER_OFFSET);
    }

    private int calculateSteps(double distance) {
        return Math.max(MIN_STEPS, (int) (distance * STEPS_PER_BLOCK));
    }

    private CurvePoint createCurvePoint(double px, double py, double pz,
                                        double tx, double ty, double tz) {
        Vector3 normalizedTangent = normalizeTangent(tx, ty, tz);
        Vector3 perpendicular = computeHorizontalPerpendicular(normalizedTangent.getX(), normalizedTangent.getZ());

        return new CurvePoint(
                Vector3.at(px, py, pz),
                normalizedTangent,
                perpendicular
        );
    }

    private Vector3 normalizeTangent(double tx, double ty, double tz) {
        double tLength = Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (tLength > EPSILON) {
            return Vector3.at(tx / tLength, ty / tLength, tz / tLength);
        }
        return Vector3.at(tx, ty, tz);
    }

    private Vector3 computeHorizontalPerpendicular(double ntx, double ntz) {
        double xzDist = Math.sqrt(ntx * ntx + ntz * ntz);
        if (xzDist > EPSILON) {
            return Vector3.at(-ntz / xzDist, 0, ntx / xzDist);
        }
        return Vector3.ZERO;
    }
}
package com.almarel.orion.curve.data;

import com.sk89q.worldedit.math.Vector3;

public record CurvePoint(Vector3 position, Vector3 tangent, Vector3 perpendicular) {
}
package com.almarel.orion.brush.data.capabilities;

public interface AxisScalable {
    // Константа только для процедурных кистей
    double PROCEDURAL_LIMIT = 10.0;

    double getScaleX(); void setScaleX(double x);
    double getScaleY(); void setScaleY(double y);
    double getScaleZ(); void setScaleZ(double z);

    double updateAxisScale(String axis, double delta);
}
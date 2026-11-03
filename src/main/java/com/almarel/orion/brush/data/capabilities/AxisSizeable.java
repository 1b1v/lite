package com.almarel.orion.brush.data.capabilities;

public interface AxisSizeable {
    double getSizeX(); void setSizeX(double x);
    double getSizeY(); void setSizeY(double y);
    double getSizeZ(); void setSizeZ(double z);

    double updateAxisSize(String axis, double delta);
}
package com.almarel.orion.brush.data.capabilities;

public interface ConstructTransformable {

    boolean isConstructScaleByRadius();
    void setConstructScaleByRadius(boolean constructScaleByRadius); // no usage

    double getConstructScaleMultiplier();
    void setConstructScaleMultiplier(double constructScaleMultiplier);

    double getConstructScaleX();
    void setConstructScaleX(double constructScaleX);

    double getConstructScaleY();
    void setConstructScaleY(double constructScaleY);

    double getConstructScaleZ();
    void setConstructScaleZ(double constructScaleZ);

    double[] getConstructRotationMatrix();
    void setConstructRotationMatrix(double[] matrix);
}
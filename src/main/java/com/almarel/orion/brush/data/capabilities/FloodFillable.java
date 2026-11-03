package com.almarel.orion.brush.data.capabilities;

import com.almarel.orion.brush.data.enums.ConnectivityMode;

public interface FloodFillable {

    boolean isFloodFillEnabled();
    void setFloodFillEnabled(boolean enabled);

    /**
     * false = Выделяем весь кубоид (Bounding Box), куда дотекла заливка.
     * true = Выделяем строго только те блоки, до которых дотекла заливка (Exact Mask).
     */
    boolean isExactSelection();
    void setExactSelection(boolean exact);

    ConnectivityMode getConnectivityMode();
    void setConnectivityMode(ConnectivityMode mode); // no usage

    int getConnectivityGap();
    void setConnectivityGap(int gap); // no usage
}
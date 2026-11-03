package com.almarel.orion.brush.data.capabilities;

import com.almarel.orion.brush.data.enums.BrushRotationMode;
import java.util.Set;

public interface RotationModeable {
    BrushRotationMode getRotationMode();
    void setRotationMode(BrushRotationMode mode);
    Set<BrushRotationMode> getAvailableRotationModes();

    /**
     * Автоматически переключает режим вращения кисти на следующий доступный.
     */
    default void cycleNextRotationMode() {
        Set<BrushRotationMode> available = getAvailableRotationModes();
        if (available == null || available.isEmpty()) return;

        BrushRotationMode current = getRotationMode();
        BrushRotationMode[] vals = BrushRotationMode.values();
        int currentOrd = current != null ? current.ordinal() : 0;

        // Ищем следующий ДОСТУПНЫЙ режим по кругу
        for (int i = 1; i <= vals.length; i++) {
            BrushRotationMode nextMode = vals[(currentOrd + i) % vals.length];
            if (available.contains(nextMode)) {
                setRotationMode(nextMode);
                return;
            }
        }
    }
}
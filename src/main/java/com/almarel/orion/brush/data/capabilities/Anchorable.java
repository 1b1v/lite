package com.almarel.orion.brush.data.capabilities;

import java.util.List;

public interface Anchorable {
    String getAnchorMode();
    void setAnchorMode(String mode);
    int getAnchorOffset();
    void setAnchorOffset(int offset);
    List<String> getAvailableAnchorModes();

    default void cycleNextAnchorMode() { // no usage
        List<String> available = getAvailableAnchorModes();
        if (available == null || available.isEmpty()) return;

        String current = getAnchorMode();
        int currentIndex = available.indexOf(current);

        // Если текущего режима нет в списке, берем первый
        if (currentIndex == -1) {
            setAnchorMode(available.get(0));
            return;
        }

        // Переключаем на следующий по кругу
        int nextIndex = (currentIndex + 1) % available.size();
        setAnchorMode(available.get(nextIndex));
    }
}
package com.almarel.orion.brush.data.capabilities;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public interface Groupable {

    String getRawActiveGroup();
    void setRawActiveGroup(String name);

    Set<String> getSelectedGroups();

    default String getActiveGroup() {
        Set<String> selected = getSelectedGroups();
        if (selected != null && !selected.isEmpty()) {
            int targetIndex = ThreadLocalRandom.current().nextInt(selected.size());
            int index = 0;
            for (String group : selected) {
                if (index++ == targetIndex) {
                    return group;
                }
            }
        }
        return getRawActiveGroup();
    }

    default void setActiveGroup(String name) {
        setRawActiveGroup(name);
    }

    default void toggleSelectedGroup(String name) {
        Set<String> selected = getSelectedGroups();
        if (selected.contains(name)) {
            selected.remove(name);
        } else {
            selected.add(name);
        }
    }

    default void clearSelectedGroups() {
        getSelectedGroups().clear();
    }
}
package com.almarel.orion.brush.data.capabilities;

import com.almarel.orion.brush.data.enums.ScrollAction;
import java.util.Set;

public interface ScrollActionable {
    ScrollAction getCurrentScrollAction();
    void setCurrentScrollAction(ScrollAction action);
    Set<ScrollAction> getActiveScrollActions();

    /**
     * Автоматически переключает действие скролла на следующее доступное.
     */
    default void cycleNextScrollAction() {
        Set<ScrollAction> active = getActiveScrollActions();
        if (active == null || active.isEmpty()) return;

        ScrollAction current = getCurrentScrollAction();
        ScrollAction[] vals = ScrollAction.values();
        int currentOrd = current != null ? current.ordinal() : 0;

        for (int i = 1; i <= vals.length; i++) {
            ScrollAction nextAction = vals[(currentOrd + i) % vals.length];
            if (active.contains(nextAction)) {
                setCurrentScrollAction(nextAction);
                return;
            }
        }
    }
}
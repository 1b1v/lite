package com.almarel.orion.brush.data.capabilities;

/**
 * Интерфейс для кистей, поддерживающих физику падения (гравитацию).
 */
public interface Gravityable {
    boolean hasGravity();
    void setGravity(boolean gravity);
}
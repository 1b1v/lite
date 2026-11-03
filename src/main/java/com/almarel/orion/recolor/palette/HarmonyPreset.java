package com.almarel.orion.recolor.palette;

/**
 * Один именованный вариант гармонии круга Иттена — угол поворота Hue
 * якорной (самой весомой) доминирующей группы постройки.
 *
 * @param displayName          человекочитаемое имя схемы (для будущего GUI)
 * @param anchorHueDeltaDegrees на сколько градусов повернуть Hue якоря
 */
public record HarmonyPreset(String displayName, double anchorHueDeltaDegrees) {
}
package com.almarel.orion.brush.presentation.gui.settings.widgets.grid;

import com.almarel.orion.brush.data.BrushState;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Двухпроходный строитель сетки (Two-Pass Layout).
 * Гарантирует, что кнопки получают свои "родные" слоты в первую очередь.
 * Коллизии (сироты) разрешаются только после того, как основная сетка построена.
 */
public class DynamicGridBuilder {

    private final BrushState state;

    // Бронирования: Слот -> Действие отрисовки
    private final Map<Integer, Runnable> exactPlacements = new HashMap<>();
    // Сироты (те, кому не хватило их любимого слота)
    private final List<Consumer<Integer>> orphans = new ArrayList<>();

    // Доступные слоты (наша матрица 3x3)
    private final List<Integer> availableSlots = new ArrayList<>(Arrays.asList(
            14, 15, 16,
            23, 24, 25,
            32, 33, 34
    ));

    public DynamicGridBuilder(BrushState state) {
        this.state = state;
    }

    public <T> DynamicGridBuilder tryInject(int preferredSlot, Class<T> featureClass, BiConsumer<Integer, T> drawAction) {
        if (featureClass.isInstance(state)) {
            T castedState = featureClass.cast(state);

            if (!exactPlacements.containsKey(preferredSlot)) {
                // Место свободно - бронируем его за этой кнопкой
                exactPlacements.put(preferredSlot, () -> drawAction.accept(preferredSlot, castedState));
            } else {
                // Место занято - отправляем в лист ожидания
                orphans.add((slot) -> drawAction.accept(slot, castedState));
            }
        }
        return this;
    }

    public DynamicGridBuilder tryInjectIf(int preferredSlot, boolean condition, Consumer<Integer> drawAction) {
        if (condition) {
            if (!exactPlacements.containsKey(preferredSlot)) {
                exactPlacements.put(preferredSlot, () -> drawAction.accept(preferredSlot));
            } else {
                orphans.add(drawAction);
            }
        }
        return this;
    }

    /**
     * [NEW] Выполняет физическую отрисовку.
     * Обязательно вызывать в самом конце цепочки tryInject!
     */
    public void commit() {
        // Этап 1: Рисуем все кнопки на их законных местах
        for (Map.Entry<Integer, Runnable> entry : exactPlacements.entrySet()) {
            entry.getValue().run();
            // Вычеркиваем занятый слот из списка доступных
            availableSlots.remove(entry.getKey());
        }

        // Этап 2: Раскидываем сирот (сглаживание, гравитацию) по оставшимся дыркам
        for (Consumer<Integer> orphan : orphans) {
            if (!availableSlots.isEmpty()) {
                int nextFreeSlot = availableSlots.remove(0); // Берем первую свободную дырку (обычно в конце)
                orphan.accept(nextFreeSlot);
            }
        }
    }
}
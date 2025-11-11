package guru.qa.ui.utils.gestures;

/**
 * Направление жеста для Android (Appium UiAutomator2).
 *
 * <p>Используется в Android-специфичных командах
 * <code>mobile:*Gesture</code> как значение параметра <code>direction</code>,
 * где требуется нижний регистр: <code>"up"</code>, <code>"down"</code>,
 * <code>"left"</code>, <code>"right"</code>.</p>
 *
 * <h3>Пример</h3>
 * <pre>{@code
 * android().executeScript("mobile: scrollGesture", Map.of(
 *     "elementId", elementId,
 *     "direction", GestureDirection.DOWN.wireValue(),
 *     "percent", 0.75
 * ));
 * }</pre>
 *
 * <p><b>EN:</b> Direction enum for Android Appium gestures. Use
 * {@link #wireValue()} to obtain the lowercase string required by
 * <code>mobile:*Gesture</code> commands.</p>
 */
public enum GestureDirection {
    /** Вверх / Up */    UP,
    /** Вниз / Down */   DOWN,
    /** Влево / Left */  LEFT,
    /** Вправо / Right */ RIGHT;

    /**
     * Нижний регистр для передачи в параметр <code>direction</code>
     * команд <code>mobile:*Gesture</code>.
     *
     * @return одно из: {@code "up"}, {@code "down"}, {@code "left"}, {@code "right"}
     */
    public String wireValue() {
        return name().toLowerCase();
    }
}

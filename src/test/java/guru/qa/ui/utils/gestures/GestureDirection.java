package guru.qa.ui.utils.gestures;

;

/**
 * Направления для Android-жестов Appium.
 * Сериализуются в нижний регистр для "mobile: *Gesture".
 */
public enum GestureDirection {
    UP, DOWN, LEFT, RIGHT;

    public String wireValue() {
        return name().toLowerCase();
    }
}

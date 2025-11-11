package guru.qa.ui.utils.gestures;

import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.Map;
import java.util.Objects;

import static guru.qa.ui.allure.Steps.step;

/**
 * Android mobile gestures (Appium 3 + UiAutomator2) — безопасные обёртки над
 * {@code driver.executeScript("mobile: <gesture>", ...)}.
 *
 * Возвраты: {@code scrollGesture}/{@code flingGesture} → {@code boolean}.
 * Никаких TouchAction/UiSelector; только современные {@code mobile:*}.
 */
@Slf4j
@UtilityClass
public class AndroidMobileGestures {

    /* ===================== БАЗА ===================== */

    /** Текущий {@link AndroidDriver}; ошибка, если сессия не Android. */
    private AndroidDriver android() {
        AppiumDriver driver = (AppiumDriver) WebDriverRunner.getWebDriver();
        if (!(driver instanceof AndroidDriver ad)) {
            throw new IllegalStateException("Текущий драйвер не AndroidDriver. Команды 'mobile:*' — Android-специфичны.");
        }
        return ad;
    }

    /** elementId с мягкой проверкой правила кликабельности. */
    private String idOf(SelenideAppiumElement el) {
        boolean displayed = el.isDisplayed();
        boolean enabled = el.isEnabled();
        boolean clickable = "true".equalsIgnoreCase(Objects.toString(el.getAttribute("clickable"), "false"));
        if (!(displayed && enabled && clickable)) {
            log.warn("Элемент может быть некликабелен (displayed={}, enabled={}, clickable={}) — жест может не сработать корректно",
                    displayed, enabled, clickable);
        }
        return ((RemoteWebElement) el.getWrappedElement()).getId();
    }

    /* ===================== CLICK / DOUBLE / LONG CLICK ===================== */

    /** {@code mobile: clickGesture} по элементу. */
    public void clickGesture(SelenideAppiumElement element) {
        step("Тап по элементу (mobile: clickGesture)", () -> {
            var result = android().executeScript("mobile: clickGesture",
                    Map.ofEntries(Map.entry("elementId", idOf(element))));
            log.debug("clickGesture result={}", result);
        });
    }

    /** {@code mobile: clickGesture} по области (тап в центр прямоугольника). */
    public void clickGesture(GestureArea area) {
        step("Тап по области (mobile: clickGesture)", () -> {
            int cx = area.left() + area.width() / 2;
            int cy = area.top() + area.height() / 2;
            var result = android().executeScript("mobile: clickGesture",
                    Map.ofEntries(
                            Map.entry("x", cx),
                            Map.entry("y", cy),
                            Map.entry("left", area.left()),
                            Map.entry("top", area.top()),
                            Map.entry("width", area.width()),
                            Map.entry("height", area.height())
                    ));
            log.debug("clickGesture (area) result={}", result);
        });
    }

    /** {@code mobile: doubleClickGesture} по элементу. */
    public void doubleClickGesture(SelenideAppiumElement element) {
        step("Двойной тап по элементу (mobile: doubleClickGesture)", () -> {
            var result = android().executeScript("mobile: doubleClickGesture",
                    Map.ofEntries(Map.entry("elementId", idOf(element))));
            log.debug("doubleClickGesture result={}", result);
        });
    }

    /** {@code mobile: longClickGesture} по элементу. */
    public void longClickGesture(SelenideAppiumElement element, long durationMs) {
        step("Долгое нажатие по элементу (mobile: longClickGesture, " + durationMs + " мс)", () -> {
            var result = android().executeScript("mobile: longClickGesture",
                    Map.ofEntries(
                            Map.entry("elementId", idOf(element)),
                            Map.entry("duration", durationMs)
                    ));
            log.debug("longClickGesture result={}", result);
        });
    }

    /* ===================== SWIPE / SCROLL / FLING ===================== */

    /**
     * {@code mobile: swipeGesture} по элементу.
     * @param percent доля размера области (0..1)
     * @param speedPxPerSec скорость (px/sec), {@code null} — не передавать
     */
    public void swipeIn(SelenideAppiumElement element, GestureDirection direction, double percent, Integer speedPxPerSec) {
        step("Свайп по элементу (mobile: swipeGesture, " + direction + ", " + percent + ", speed=" + speedPxPerSec + ")", () -> {
            if (speedPxPerSec == null) {
                android().executeScript("mobile: swipeGesture",
                        Map.ofEntries(
                                Map.entry("elementId", idOf(element)),
                                Map.entry("direction", direction.wireValue()),
                                Map.entry("percent", percent)
                        ));
            } else {
                android().executeScript("mobile: swipeGesture",
                        Map.ofEntries(
                                Map.entry("elementId", idOf(element)),
                                Map.entry("direction", direction.wireValue()),
                                Map.entry("percent", percent),
                                Map.entry("speed", speedPxPerSec)
                        ));
            }
        });
    }

    /** {@code mobile: swipeGesture} по области. */
    public void swipeIn(GestureArea area, GestureDirection direction, double percent, Integer speedPxPerSec) {
        step("Свайп по области (mobile: swipeGesture, " + direction + ", " + percent + ", speed=" + speedPxPerSec + ")", () -> {
            if (speedPxPerSec == null) {
                android().executeScript("mobile: swipeGesture",
                        Map.ofEntries(
                                Map.entry("left", area.left()),
                                Map.entry("top", area.top()),
                                Map.entry("width", area.width()),
                                Map.entry("height", area.height()),
                                Map.entry("direction", direction.wireValue()),
                                Map.entry("percent", percent)
                        ));
            } else {
                android().executeScript("mobile: swipeGesture",
                        Map.ofEntries(
                                Map.entry("left", area.left()),
                                Map.entry("top", area.top()),
                                Map.entry("width", area.width()),
                                Map.entry("height", area.height()),
                                Map.entry("direction", direction.wireValue()),
                                Map.entry("percent", percent),
                                Map.entry("speed", speedPxPerSec)
                        ));
            }
        });
    }

    /**
     * {@code mobile: scrollGesture} по элементу.
     * @return {@code true}, если фактически прокрутилось
     */
    public boolean scrollIn(SelenideAppiumElement element, GestureDirection direction, double percent, Integer speedPxPerSec) {
        return step("Скролл по элементу (mobile: scrollGesture, " + direction + ", " + percent + ", speed=" + speedPxPerSec + ")", () -> {
            Object result =
                    (speedPxPerSec == null)
                            ? android().executeScript("mobile: scrollGesture",
                            Map.ofEntries(
                                    Map.entry("elementId", idOf(element)),
                                    Map.entry("direction", direction.wireValue()),
                                    Map.entry("percent", percent)
                            ))
                            : android().executeScript("mobile: scrollGesture",
                            Map.ofEntries(
                                    Map.entry("elementId", idOf(element)),
                                    Map.entry("direction", direction.wireValue()),
                                    Map.entry("percent", percent),
                                    Map.entry("speed", speedPxPerSec)
                            ));
            boolean scrolled = Boolean.TRUE.equals(result);
            log.debug("scrollGesture result={}", scrolled);
            return scrolled;
        });
    }

    /** {@code mobile: scrollGesture} по области. */
    public boolean scrollIn(GestureArea area, GestureDirection direction, double percent, Integer speedPxPerSec) {
        return step("Скролл по области (mobile: scrollGesture, " + direction + ", " + percent + ", speed=" + speedPxPerSec + ")", () -> {
            Object result =
                    (speedPxPerSec == null)
                            ? android().executeScript("mobile: scrollGesture",
                            Map.ofEntries(
                                    Map.entry("left", area.left()),
                                    Map.entry("top", area.top()),
                                    Map.entry("width", area.width()),
                                    Map.entry("height", area.height()),
                                    Map.entry("direction", direction.wireValue()),
                                    Map.entry("percent", percent)
                            ))
                            : android().executeScript("mobile: scrollGesture",
                            Map.ofEntries(
                                    Map.entry("left", area.left()),
                                    Map.entry("top", area.top()),
                                    Map.entry("width", area.width()),
                                    Map.entry("height", area.height()),
                                    Map.entry("direction", direction.wireValue()),
                                    Map.entry("percent", percent),
                                    Map.entry("speed", speedPxPerSec)
                            ));
            boolean scrolled = Boolean.TRUE.equals(result);
            log.debug("scrollGesture (area) result={}", scrolled);
            return scrolled;
        });
    }

    /**
     * {@code mobile: flingGesture} — быстрый «бросок».
     * @return {@code true}, если можно продолжать скроллить дальше
     */
    public boolean flingIn(SelenideAppiumElement element, GestureDirection direction, Integer speedPxPerSec) {
        return step("Флинг по элементу (mobile: flingGesture, " + direction + ", speed=" + speedPxPerSec + ")", () -> {
            Object result =
                    (speedPxPerSec == null)
                            ? android().executeScript("mobile: flingGesture",
                            Map.ofEntries(
                                    Map.entry("elementId", idOf(element)),
                                    Map.entry("direction", direction.wireValue())
                            ))
                            : android().executeScript("mobile: flingGesture",
                            Map.ofEntries(
                                    Map.entry("elementId", idOf(element)),
                                    Map.entry("direction", direction.wireValue()),
                                    Map.entry("speed", speedPxPerSec)
                            ));
            boolean canScrollMore = Boolean.TRUE.equals(result);
            log.debug("flingGesture result(canScrollMore)={}", canScrollMore);
            return canScrollMore;
        });
    }

    /** {@code mobile: flingGesture} по области. */
    public boolean flingIn(GestureArea area, GestureDirection direction, Integer speedPxPerSec) {
        return step("Флинг по области (mobile: flingGesture, " + direction + ", speed=" + speedPxPerSec + ")", () -> {
            Object result =
                    (speedPxPerSec == null)
                            ? android().executeScript("mobile: flingGesture",
                            Map.ofEntries(
                                    Map.entry("left", area.left()),
                                    Map.entry("top", area.top()),
                                    Map.entry("width", area.width()),
                                    Map.entry("height", area.height()),
                                    Map.entry("direction", direction.wireValue())
                            ))
                            : android().executeScript("mobile: flingGesture",
                            Map.ofEntries(
                                    Map.entry("left", area.left()),
                                    Map.entry("top", area.top()),
                                    Map.entry("width", area.width()),
                                    Map.entry("height", area.height()),
                                    Map.entry("direction", direction.wireValue()),
                                    Map.entry("speed", speedPxPerSec)
                            ));
            boolean canScrollMore = Boolean.TRUE.equals(result);
            log.debug("flingGesture (area) result(canScrollMore)={}", canScrollMore);
            return canScrollMore;
        });
    }

    /* ===================== DRAG & PINCH ===================== */

    /** {@code mobile: dragGesture} — перетащить от элемента к указанным координатам. */
    public void dragFrom(SelenideAppiumElement element, int endX, int endY, Integer speedPxPerSec) {
        step("Drag&Drop (mobile: dragGesture) от элемента к (" + endX + "," + endY + "), speed=" + speedPxPerSec, () -> {
            if (speedPxPerSec == null) {
                android().executeScript("mobile: dragGesture",
                        Map.ofEntries(
                                Map.entry("elementId", idOf(element)),
                                Map.entry("endX", endX),
                                Map.entry("endY", endY)
                        ));
            } else {
                android().executeScript("mobile: dragGesture",
                        Map.ofEntries(
                                Map.entry("elementId", idOf(element)),
                                Map.entry("endX", endX),
                                Map.entry("endY", endY),
                                Map.entry("speed", speedPxPerSec)
                        ));
            }
        });
    }

    /** {@code mobile: pinchOpenGesture} — увеличить масштаб. */
    public void pinchOpen(SelenideAppiumElement element, double percent, Integer speedPxPerSec) {
        step("Pinch Open (mobile: pinchOpenGesture, " + percent + ", speed=" + speedPxPerSec + ") по элементу", () -> {
            if (speedPxPerSec == null) {
                android().executeScript("mobile: pinchOpenGesture",
                        Map.ofEntries(
                                Map.entry("elementId", idOf(element)),
                                Map.entry("percent", percent)
                        ));
            } else {
                android().executeScript("mobile: pinchOpenGesture",
                        Map.ofEntries(
                                Map.entry("elementId", idOf(element)),
                                Map.entry("percent", percent),
                                Map.entry("speed", speedPxPerSec)
                        ));
            }
        });
    }

    /** {@code mobile: pinchCloseGesture} — уменьшить масштаб. */
    public void pinchClose(SelenideAppiumElement element, double percent, Integer speedPxPerSec) {
        step("Pinch Close (mobile: pinchCloseGesture, " + percent + ", speed=" + speedPxPerSec + ") по элементу", () -> {
            if (speedPxPerSec == null) {
                android().executeScript("mobile: pinchCloseGesture",
                        Map.ofEntries(
                                Map.entry("elementId", idOf(element)),
                                Map.entry("percent", percent)
                        ));
            } else {
                android().executeScript("mobile: pinchCloseGesture",
                        Map.ofEntries(
                                Map.entry("elementId", idOf(element)),
                                Map.entry("percent", percent),
                                Map.entry("speed", speedPxPerSec)
                        ));
            }
        });
    }

    /* ===================== ВЫСОКИЙ УРОВЕНЬ ===================== */

    /** Доскроллить контейнер до самого низа: серия {@code fling} и завершающий {@code scroll}. */
    public void scrollToBottom(SelenideAppiumElement container) {
        step("Доскроллить контейнер до самого низа (fling → scroll)", () -> {
            boolean canScrollMore = true;
            while (canScrollMore) {
                canScrollMore = flingIn(container, GestureDirection.DOWN, null);
            }
            scrollIn(container, GestureDirection.DOWN, 1.0, null);
        });
    }

    /**
     * Прокрутить N шагов в выбранную сторону.
     * @param steps количество шагов
     * @param percentPerStep доля прокрутки за шаг (0..1)
     */
    public void scrollNSteps(SelenideAppiumElement container, GestureDirection direction, int steps, double percentPerStep) {
        step("Скроллить " + steps + " шаг(ов) " + direction + " по контейнеру (percentPerStep=" + percentPerStep + ")", () -> {
            for (int i = 0; i < steps; i++) {
                if (!scrollIn(container, direction, percentPerStep, null)) break;
            }
        });
    }
}

package guru.qa.ui.utils.gestures;

import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Step;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.Map;
import java.util.Objects;

/*
==================================== 🎯 MASTER PROMPT (Android Mobile Gestures) ====================================

ЦЕЛЬ
— Дать простую, расширяемую и безопасную обёртку над Android UiAutomator2 "Platform-Specific Extensions"
  (команды вида `driver.executeScript("mobile: <method>", args)`), чтобы использовать жесты в автотестах
  без устаревших API (TouchAction/UiSelector) и без «магических» координат.

КОНТЕКСТ ПРОЕКТА
— Appium Server 3.x + UiAutomator2; Java 21; Gradle; Selenide-Appium.
— Элементы в PageObject'ах — SelenideAppiumElement (инвариант проекта).
— Локаторы: предпочтительно resource-id / accessibilityId; XPath 2.0 только при необходимости.
— Правило кликабельности: действие с элементом выполняем, только если displayed==true, enabled==true и attribute(clickable)==true.

ДИЗАЙН / API
— Явно используем AppiumDriver и AndroidDriver:
      var result = androidDriver.executeScript("mobile: <method>", Map.ofEntries(...));
— Методы предоставляют обёртки над основными жестами:
      clickGesture / doubleClickGesture / longClickGesture /
      swipeGesture / scrollGesture / flingGesture /
      dragGesture / pinchOpenGesture / pinchCloseGesture
— Доступны перегрузки «по элементу» (elementId) и «по области» (left/top/width/height) — через record GestureArea.
— Возвраты строго отражают поведение драйвера:
      scrollGesture -> boolean (фактически прокрутилось?)
      flingGesture  -> boolean (можно ли ещё скроллить дальше?)
— Логирование и шаги Allure (@Step) на русском; без Thread.sleep.

ПАТТЕРНЫ ВЫСОКОГО УРОВНЯ (готовые сценарии)
— scrollToBottom(container): fling до упора, затем заключительный scroll.
— scrollNSteps(container, direction, steps, percent): «N шагов» управляемой прокрутки.

БЕЗОПАСНОСТЬ И УСТОЙЧИВОСТЬ
— Перед действиями по элементу выводим предупреждение, если нарушено правило кликабельности
  (displayed, enabled, attribute(clickable)="true").
— Никаких TouchAction/UiSelector; только современные "mobile:*" команды.
— Исключение IllegalStateException, если текущий драйвер не AndroidDriver.

РАСШИРЕНИЕ
— Можно добавить: «доскроллить до элемента по условию/локатору», «жесты по вьюпорту» (GestureArea.fromViewport()),
  обёртки с таймаутами ожиданий/ретраями, а также iOS-вариант в отдельном классе (через IOSDriver).

ОГРАНИЧЕНИЯ / НЕ ЦЕЛИ
— Класс не занимается ожиданиями видимости/готовности элементов — это обязанность вызывающей стороны (Selenide Conditions).
— Класс не изменяет глобальные таймауты/капабилити и не управляет жизненным циклом драйвера.

ПРИМЕРЫ ВЫЗОВА
  import static ru.rgs.mobile.core.gestures.AndroidMobileGestures.*;
  import static ru.rgs.mobile.core.gestures.GestureDirection.*;

  var feed = com.codeborne.selenide.appium.SelenideAppium.$(io.appium.java_client.AppiumBy.id("org.wikipedia.alpha:id/feed_view"));

  clickGesture(feed);                                   // надёжный «тап» по элементу
  swipeIn(feed, LEFT, 0.6, null);                       // свайп влево на 60% ширины
  longClickGesture(feed, 800);                          // долгое нажатие 800 мс
  boolean more = flingIn(feed, DOWN, null);             // есть ли ещё куда скроллить вниз?
  scrollToBottom(feed);                                 // доскроллить ленту «до конца»
  pinchOpen(feed, 0.75, null);                          // зум+
  dragFrom(feed, 100, 400, 3000);                       // drag к координатам (100,400) со speed=3000 px/s

=====================================================================================================================
*/

@Slf4j
@UtilityClass
public class AndroidMobileGestures {

    /* ===================== БАЗА: получение AndroidDriver ===================== */

    /**
     * Возвращает текущий AndroidDriver. Кидает IllegalStateException, если сессия не Android.
     */
    private AndroidDriver android() {
        AppiumDriver driver = (AppiumDriver) WebDriverRunner.getWebDriver();
        if (!(driver instanceof AndroidDriver ad)) {
            throw new IllegalStateException("Текущий драйвер не AndroidDriver. Команды 'mobile: *Gesture' — Android-специфичны.");
        }
        return ad;
    }

    /**
     * Безопасно получает elementId и предупреждает, если элемент не кликабелен по инварианту.
     */
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

    /**
     * "mobile: clickGesture" по элементу.
     */
    @Step("Тап по элементу (mobile: clickGesture)")
    public void clickGesture(SelenideAppiumElement element) {
        var result = android().executeScript("mobile: clickGesture",
                Map.ofEntries(Map.entry("elementId", idOf(element))));
        log.debug("clickGesture result={}", result);
    }

    /**
     * "mobile: clickGesture" по области (тап в центр прямоугольника).
     */
    @Step("Тап по области (mobile: clickGesture)")
    public void clickGesture(GestureArea area) {
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
    }

    /**
     * "mobile: doubleClickGesture" по элементу.
     */
    @Step("Двойной тап по элементу (mobile: doubleClickGesture)")
    public void doubleClickGesture(SelenideAppiumElement element) {
        var result = android().executeScript("mobile: doubleClickGesture",
                Map.ofEntries(Map.entry("elementId", idOf(element))));
        log.debug("doubleClickGesture result={}", result);
    }

    /**
     * "mobile: longClickGesture" по элементу. @param durationMs длительность удержания в мс.
     */
    @Step("Долгое нажатие по элементу (mobile: longClickGesture, {durationMs} мс)")
    public void longClickGesture(SelenideAppiumElement element, long durationMs) {
        var result = android().executeScript("mobile: longClickGesture",
                Map.ofEntries(
                        Map.entry("elementId", idOf(element)),
                        Map.entry("duration", durationMs)
                ));
        log.debug("longClickGesture result={}", result);
    }

    /* ===================== SWIPE / SCROLL / FLING ===================== */

    /**
     * "mobile: swipeGesture" по элементу.
     *
     * @param percent       доля размера области (0..1)
     * @param speedPxPerSec скорость (px/sec), можно null — тогда аргумент опускаем
     */
    @Step("Свайп по элементу (mobile: swipeGesture, {direction}, {percent}, speed={speedPxPerSec})")
    public void swipeIn(SelenideAppiumElement element, GestureDirection direction, double percent, Integer speedPxPerSec) {
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
    }

    /**
     * "mobile: swipeGesture" по области.
     */
    @Step("Свайп по области (mobile: swipeGesture, {direction}, {percent}, speed={speedPxPerSec})")
    public void swipeIn(GestureArea area, GestureDirection direction, double percent, Integer speedPxPerSec) {
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
    }

    /**
     * "mobile: scrollGesture" по элементу.
     *
     * @return true — если реально прокрутилось (можно использовать в циклах)
     */
    @Step("Скролл по элементу (mobile: scrollGesture, {direction}, {percent}, speed={speedPxPerSec})")
    public boolean scrollIn(SelenideAppiumElement element, GestureDirection direction, double percent, Integer speedPxPerSec) {
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
    }

    /**
     * "mobile: scrollGesture" по области.
     */
    @Step("Скролл по области (mobile: scrollGesture, {direction}, {percent}, speed={speedPxPerSec})")
    public boolean scrollIn(GestureArea area, GestureDirection direction, double percent, Integer speedPxPerSec) {
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
    }

    /**
     * "mobile: flingGesture" — быстрый «бросок».
     *
     * @return true — если можно продолжать скроллить дальше в указанном направлении
     */
    @Step("Флинг по элементу (mobile: flingGesture, {direction}, speed={speedPxPerSec})")
    public boolean flingIn(SelenideAppiumElement element, GestureDirection direction, Integer speedPxPerSec) {
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
    }

    /**
     * "mobile: flingGesture" по области.
     */
    @Step("Флинг по области (mobile: flingGesture, {direction}, speed={speedPxPerSec})")
    public boolean flingIn(GestureArea area, GestureDirection direction, Integer speedPxPerSec) {
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
    }

    /* ===================== DRAG & DROP / PINCH ===================== */

    /**
     * "mobile: dragGesture" — перетащить от элемента к конечным координатам.
     */
    @Step("Drag&Drop (mobile: dragGesture) от элемента к ({endX},{endY}), speed={speedPxPerSec}")
    public void dragFrom(SelenideAppiumElement element, int endX, int endY, Integer speedPxPerSec) {
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
    }

    /**
     * "mobile: pinchOpenGesture" — увеличить масштаб. percent 0..1
     */
    @Step("Pinch Open (mobile: pinchOpenGesture, {percent}, speed={speedPxPerSec}) по элементу")
    public void pinchOpen(SelenideAppiumElement element, double percent, Integer speedPxPerSec) {
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
    }

    /**
     * "mobile: pinchCloseGesture" — уменьшить масштаб. percent 0..1
     */
    @Step("Pinch Close (mobile: pinchCloseGesture, {percent}, speed={speedPxPerSec}) по элементу")
    public void pinchClose(SelenideAppiumElement element, double percent, Integer speedPxPerSec) {
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
    }

    /* ===================== ПАТТЕРНЫ ВЫСОКОГО УРОВНЯ ===================== */

    /**
     * Доскроллить контейнер до конца: сперва бросками (fling), затем финальный scroll.
     */
    @Step("Доскроллить контейнер до самого низа (fling → scroll)")
    public void scrollToBottom(SelenideAppiumElement container) {
        boolean canScrollMore = true;
        while (canScrollMore) {
            canScrollMore = flingIn(container, GestureDirection.DOWN, null);
        }
        scrollIn(container, GestureDirection.DOWN, 1.0, null);
    }

    /**
     * Прокрутить N шагов в выбранную сторону, по percent за шаг.
     */
    @Step("Скроллить {steps} шаг(ов) {direction} по контейнеру (percentPerStep={percentPerStep})")
    public void scrollNSteps(SelenideAppiumElement container, GestureDirection direction, int steps, double percentPerStep) {
        for (int i = 0; i < steps; i++) {
            if (!scrollIn(container, direction, percentPerStep, null)) break;
        }
    }
}

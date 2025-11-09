package guru.qa.ui.utils.gestures;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.qameta.allure.Step;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Rectangle;

import java.time.Duration;

/*
================================= 🎯 MASTER PROMPT (Scroll Into View with Selenide.Wait) ===============================

ЦЕЛЬ
— Реализовать устойчивый «доскролл до элемента» в мобильном приложении: сначала сделать элемент видимым скроллом вниз
  по заданному контейнеру, затем гарантировать, что элемент полностью попал в область контейнера (не обрезан).

КОНТЕКСТ / ИНВАРИАНТЫ
— Appium 3 + UIAutomator2, Java 21, Gradle; элементы — SelenideAppiumElement.
— Стабильные локаторы: resource-id / accessibilityId / XPath 2.0 (при необходимости).
— Жесты выполняем через AndroidMobileGestures (mobile: *Gesture). Без TouchAction / UiSelector.
— Без Thread.sleep — только Selenide.Wait() с polling.

АЛГОРИТМ
1) Внутри Selenide.Wait(): если target НЕ виден → скроллим контейнер ВНИЗ (scrollGesture DOWN, percent=0.7) и продолжаем.
2) Как только target виден → проверяем «полную видимость» внутри контейнера:
      - если верх элемента выше верхней границы контейнера → небольшой скролл ВВЕРХ;
      - если низ элемента ниже нижней границы контейнера → небольшой скролл ВНИЗ;
   повторяем, пока элемент не окажется целиком внутри или не упрёмся в границы (scrollGesture вернёт false).
3) Условие выхода из ожидания — элемент «полностью виден».

ПАРАМЕТРЫ
— timeout: общий таймаут ожидания; polling=250ms.
— percentPerStep: доля высоты контейнера для шага скролла (по умолчанию 0.7 при поиске элемента; для «дотяжки» вычисляется).

ОГРАНИЧЕНИЯ
— Если контейнер исчерпал прокрутку и target так и не появился, будет TimeoutException Selenide.Wait().
— Метод не кликает; проверка кликабельности — в ваших шагах перед кликом.

=========================================================================================================================
*/

@Slf4j
@UtilityClass
public class ScrollIntoView {

    /**
     * Удобный таймаут по умолчанию.
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(90);

    /**
     * Минимальная/максимальная доля высоты контейнера для «выравнивающих» скроллов.
     */
    private static final double MIN_ADJUST_PERCENT = 0.15;
    private static final double MAX_ADJUST_PERCENT = 0.85;

    /**
     * Главный метод: скроллит вниз, пока элемент не станет видим, затем выравнивает, чтобы он был целиком в контейнере.
     */
    @Step("Прокрутить контейнер до полной видимости элемента (внутри Selenide.Wait)")
    public void intoView(SelenideAppiumElement scrollableContainer, SelenideAppiumElement target) {
        intoView(scrollableContainer, target, DEFAULT_TIMEOUT);
    }

    /**
     * То же самое, но с настраиваемым таймаутом.
     */
    @Step("Прокрутить контейнер до полной видимости элемента (таймаут: {timeout})")
    public void intoView(SelenideAppiumElement scrollableContainer, SelenideAppiumElement target, Duration timeout) {
        // Весь алгоритм — внутри Selenide.Wait()
        Selenide.Wait()
                .withTimeout(timeout)
                .pollingEvery(Duration.ofMillis(250))
                .until(driver -> {
                    // 1) Если элемент ещё не виден — скроллим ВНИЗ на крупный шаг
                    if (!safeDisplayed(target)) {
                        boolean scrolled = AndroidMobileGestures.scrollIn(
                                scrollableContainer, GestureDirection.DOWN, 0.5, null);
                        log.debug("search phase: displayed=false, scrolledDown={}", scrolled);
                        return false; // Продолжаем ждать
                    }

                    // 2) Элемент виден — проверяем полную видимость в контейнере
                    if (isFullyVisibleIn(scrollableContainer, target)) {
                        log.debug("target is fully visible in container");
                        return true; // Условие выполнено
                    }

                    // 3) Элемент частично обрезан — дотягиваем
                    Rectangle cr = scrollableContainer.getRect();
                    Rectangle er = target.getRect();
                    int containerTop = cr.getY();
                    int containerBottom = cr.getY() + cr.getHeight();
                    int elemTop = er.getY();
                    int elemBottom = er.getY() + er.getHeight();

                    if (elemTop < containerTop) {
                        // Верх элемента выше контейнера → лёгкий скролл ВВЕРХ
                        double percent = clamp(((containerTop - elemTop) / (double) cr.getHeight()) + 0.1,
                                MIN_ADJUST_PERCENT, MAX_ADJUST_PERCENT);
                        boolean scrolledUp = AndroidMobileGestures.scrollIn(
                                scrollableContainer, GestureDirection.UP, percent, null);
                        log.debug("adjust phase: top overflow, percent={}, scrolledUp={}", percent, scrolledUp);
                        return false;
                    }

                    if (elemBottom > containerBottom) {
                        // Низ элемента ниже контейнера → лёгкий скролл ВНИЗ
                        double percent = clamp(((elemBottom - containerBottom) / (double) cr.getHeight()) + 0.1,
                                MIN_ADJUST_PERCENT, MAX_ADJUST_PERCENT);
                        boolean scrolledDown = AndroidMobileGestures.scrollIn(
                                scrollableContainer, GestureDirection.DOWN, percent, null);
                        log.debug("adjust phase: bottom overflow, percent={}, scrolledDown={}", percent, scrolledDown);
                        return false;
                    }

                    // На всякий случай: если добрались сюда, считаем, что всё ок
                    return true;
                });
    }

    /* ============================== HELPERS ============================== */

    private boolean safeDisplayed(SelenideAppiumElement el) {
        try {
            return el.isDisplayed();
        } catch (RuntimeException e) {
            // На случай переотрисовки/рецикла вьюх между поллингами
            log.trace("safeDisplayed(): transient error: {}", e.toString());
            return false;
        }
    }

    /**
     * Полная видимость: верх элемента не выше верхней границы контейнера, низ не ниже нижней границы.
     */
    private boolean isFullyVisibleIn(SelenideAppiumElement container, SelenideAppiumElement element) {
        Rectangle cr = container.getRect();
        Rectangle er = element.getRect();
        int containerTop = cr.getY();
        int containerBottom = cr.getY() + cr.getHeight();
        int elemTop = er.getY();
        int elemBottom = er.getY() + er.getHeight();
        boolean fully = elemTop >= containerTop && elemBottom <= containerBottom && safeDisplayed(element);
        if (!fully) {
            log.debug("full visibility check: elemTop={}, elemBottom={}, containerTop={}, containerBottom={}, result={}",
                    elemTop, elemBottom, containerTop, containerBottom, fully);
        }
        return fully;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}

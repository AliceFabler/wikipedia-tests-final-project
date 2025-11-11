package guru.qa.ui.utils.gestures;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Rectangle;

import java.time.Duration;

import static guru.qa.ui.allure.Steps.step;

/**
 * Утилита «доскролла» до элемента в пределах контейнера.
 *
 * <p>Алгоритм:
 * <ol>
 *   <li>Внутри {@code Selenide.Wait()} скроллим контейнер ВНИЗ, пока целевой элемент не станет видимым.</li>
 *   <li>Когда элемент видим — «дотягиваем» его так, чтобы он полностью поместился в границы контейнера
 *       (не обрезан сверху/снизу): при необходимости делаем короткие скроллы ВВЕРХ/ВНИЗ.</li>
 * </ol>
 *
 * <p>Инварианты проекта: Appium 3 + UiAutomator2; жесты — через {@link AndroidMobileGestures};
 * никаких {@code Thread.sleep} — только ожидания и поллинг.</p>
 *
 * <h3>Использование</h3>
 * <pre>{@code
 * ScrollIntoView.intoView(feedContainer, targetCard, Duration.ofSeconds(60));
 * }</pre>
 */
@Slf4j
@UtilityClass
public class ScrollIntoView {

    /** Таймаут по умолчанию. */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(90);

    /** Диапазон долей высоты контейнера для «выравнивающих» скроллов. */
    private static final double MIN_ADJUST_PERCENT = 0.15;
    private static final double MAX_ADJUST_PERCENT = 0.85;

    /**
     * Прокрутить контейнер до полной видимости элемента (таймаут по умолчанию).
     *
     * @param scrollableContainer прокручиваемый контейнер
     * @param target              целевой элемент
     */
    public void intoView(SelenideAppiumElement scrollableContainer, SelenideAppiumElement target) {
        step("Прокрутить контейнер до полной видимости элемента", () ->
                intoView(scrollableContainer, target, DEFAULT_TIMEOUT)
        );
    }

    /**
     * Прокрутить контейнер до полной видимости элемента (с заданным таймаутом).
     *
     * @param scrollableContainer прокручиваемый контейнер
     * @param target              целевой элемент
     * @param timeout             общий таймаут ожидания
     */
    public void intoView(SelenideAppiumElement scrollableContainer,
                         SelenideAppiumElement target,
                         Duration timeout) {
        step("Прокрутить контейнер до полной видимости элемента (таймаут: " + timeout.toSeconds() + " с)", () ->
                Selenide.Wait()
                        .withTimeout(timeout)
                        .pollingEvery(Duration.ofMillis(250))
                        .until(driver -> {
                            // 1) Фаза поиска: крутим вниз, пока элемент не станет видимым
                            if (!safeDisplayed(target)) {
                                boolean scrolled = AndroidMobileGestures.scrollIn(
                                        scrollableContainer, GestureDirection.DOWN, 0.5, null);
                                log.debug("search phase: displayed=false, scrolledDown={}", scrolled);
                                return false;
                            }

                            // 2) Проверка полной видимости внутри контейнера
                            if (isFullyVisibleIn(scrollableContainer, target)) {
                                log.debug("target is fully visible in container");
                                return true;
                            }

                            // 3) Дотяжка: корректируем позицию короткими скроллами
                            Rectangle cr = scrollableContainer.getRect();
                            Rectangle er = target.getRect();
                            int containerTop = cr.getY();
                            int containerBottom = cr.getY() + cr.getHeight();
                            int elemTop = er.getY();
                            int elemBottom = er.getY() + er.getHeight();

                            if (elemTop < containerTop) {
                                double percent = clamp(((containerTop - elemTop) / (double) cr.getHeight()) + 0.1,
                                        MIN_ADJUST_PERCENT, MAX_ADJUST_PERCENT);
                                boolean up = AndroidMobileGestures.scrollIn(
                                        scrollableContainer, GestureDirection.UP, percent, null);
                                log.debug("adjust phase: top overflow, percent={}, scrolledUp={}", percent, up);
                                return false;
                            }

                            if (elemBottom > containerBottom) {
                                double percent = clamp(((elemBottom - containerBottom) / (double) cr.getHeight()) + 0.1,
                                        MIN_ADJUST_PERCENT, MAX_ADJUST_PERCENT);
                                boolean down = AndroidMobileGestures.scrollIn(
                                        scrollableContainer, GestureDirection.DOWN, percent, null);
                                log.debug("adjust phase: bottom overflow, percent={}, scrolledDown={}", percent, down);
                                return false;
                            }

                            return true;
                        })
        );
    }

    // ----------------------- Helpers -----------------------

    private boolean safeDisplayed(SelenideAppiumElement el) {
        try {
            return el.isDisplayed();
        } catch (RuntimeException e) {
            // возможная переотрисовка между тиками поллинга
            log.trace("safeDisplayed(): transient error: {}", e.toString());
            return false;
        }
    }

    /** Полная видимость: элемент целиком внутри вертикальных границ контейнера и видим. */
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

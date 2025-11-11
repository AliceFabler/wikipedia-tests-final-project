package guru.qa.ui.screens.components;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.appium.java_client.AppiumBy;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;

/**
 * Компонент для закрытия возможных оверлеев, мешающих взаимодействию.
 *
 * <p>Обрабатывает типовые случаи:
 * игры Википедии (крестик), подсказки/тур “Понятно / Got it”,
 * объявление «Настройте свою ленту».</p>
 *
 * <p><b>EN:</b> Helper to dismiss disruptive overlays: Wikipedia Games dialog,
 * “Got it” tooltips, and “Tune your feed” announcement.</p>
 */
@Slf4j
public class WikiOverlays {

    private final SelenideAppiumElement gamesClose =
            $(AppiumBy.id("org.wikipedia.alpha:id/closeButton"));

    private final SelenideAppiumElement gotItByText =
            $(AppiumBy.xpath("//*[matches(lower-case(@text),'^понятно$|^got it$')]"));

    private final SelenideAppiumElement announceContainer =
            $(AppiumBy.id("org.wikipedia.alpha:id/view_announcement_container"));
    private final SelenideAppiumElement announceNegative =
            $(AppiumBy.id("org.wikipedia.alpha:id/view_announcement_action_negative"));

    /**
     * Закрыть все обнаруженные оверлеи, если они показаны.
     *
     * <p><b>EN:</b> Dismiss all known overlays if visible.</p>
     */
    public void closeAllIfShown() {
        step("Закрыть все мешающие оверлеи, если они показаны", () -> {
            tryClose(announceContainer, announceNegative, "Announcement");
            tryClick(gamesClose, "WikiGames close");
            tryClick(gotItByText, "Got it");
        });
    }

    /**
     * Закрыть объявление (если контейнер появился).
     *
     * @param container корневой контейнер объявления
     * @param closeBtn  кнопка негативного действия
     * @param name      человекочитаемое имя для логов
     */
    private void tryClose(SelenideAppiumElement container, SelenideAppiumElement closeBtn, String name) {
        if (container.is(Condition.appear, Duration.ofSeconds(5))) {
            closeBtn.shouldBe(Condition.visible).click();
        }
    }

    /**
     * Нажать на элемент, если он существует.
     *
     * @param el   элемент
     * @param name имя для логов
     */
    private void tryClick(SelenideAppiumElement el, String name) {
        if (el.exists()) el.click();
    }
}

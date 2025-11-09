package guru.qa.ui.screens.components;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.appium.java_client.AppiumBy;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;

/**
 * Закрытие возможных оверлеев, мешающих кликам:
 * - Попап "Игры Википедии" (крестик);
 * - Подсказки/тур «Понятно»;
 * - Объявление "Настройте свою ленту".
 */
@Slf4j
public class WikiOverlays {

    // Игры Википедии
    private final SelenideAppiumElement gamesClose =
            $(AppiumBy.id("org.wikipedia.alpha:id/closeButton"));

    // Подсказки «Понятно»
    private final SelenideAppiumElement gotItByText =
            $(AppiumBy.xpath("//*[matches(lower-case(@text),'^понятно$|^got it$')]"));

    // Объявление в Explore
    private final SelenideAppiumElement announceContainer =
            $(AppiumBy.id("org.wikipedia.alpha:id/view_announcement_container"));
    private final SelenideAppiumElement announceNegative =
            $(AppiumBy.id("org.wikipedia.alpha:id/view_announcement_action_negative"));

    public void closeAllIfShown() {
        step("Закрыть все мешающие оверлеи, если они показаны", () -> {
            tryClose(announceContainer, announceNegative, "Announcement");
            tryClick(gamesClose, "WikiGames close");
            tryClick(gotItByText, "Got it");
        });
    }

    private void tryClose(SelenideAppiumElement container, SelenideAppiumElement closeBtn, String name) {
        if (container.is(Condition.appear, Duration.ofSeconds(5))) {
            closeBtn.shouldBe(Condition.visible).click();
        }
    }

    private void tryClick(SelenideAppiumElement el, String name) {
        if (el.exists()) el.click();
    }
}

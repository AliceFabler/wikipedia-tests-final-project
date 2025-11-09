package guru.qa.ui.screens;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import guru.qa.ui.screens.components.ArticleOverflowMenu;
import io.appium.java_client.AppiumBy;
import lombok.extern.slf4j.Slf4j;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;

/*
 * 🎯 MASTER PROMPT (final)
 * ArticleScreen для Wikipedia Android:
 *  — Appium 3 + UIAutomator2; устойчивые локаторы: id/accId; XPath2 только как запасной.
 *  — Нижнее action-меню: Save/Language/Find/Theme/Contents.
 *  — Блоки «Об этой статье» и «Подробнее».
 *  — Методы с @Step RU/EN и because-сообщениями, без UiSelector.
 */

/**
 * # Экран статьи / Article screen
 * <p>
 * Цели: действия из нижней панели, переход к оглавлению, навигация по блокам «Об этой статье» и «Подробнее».
 * <p>
 * Примечание: автоскролл до глубоких элементов не реализован здесь умышленно (мы избегаем UiSelector).
 * Если элемент не в видимой области, прокрутку выполняем на уровне шагов/утилит жестов.
 */
@Slf4j
public class ArticleScreen {

    // ─────────────── Bottom actions (всегда с id) ───────────────
    private final SelenideAppiumElement saveButton =
            $(AppiumBy.id("org.wikipedia.alpha:id/page_save"));
    private final SelenideAppiumElement contentsButton =
            $(AppiumBy.id("org.wikipedia.alpha:id/page_contents"));
    // ─────────────── Top bar (overflow / tabs — id может различаться, даём fallback) ───────────────
    private final SelenideAppiumElement overflowTopButton =
            $(AppiumBy.id("org.wikipedia.alpha:id/page_toolbar_button_show_overflow_menu")); // если совпадает с Saved
    ArticleOverflowMenu overflow = new ArticleOverflowMenu();

    // ─────────────────────────── Steps ───────────────────────────

    public ArticleScreen shouldBeOpen() {
        return step("Экран статьи открыт", () -> {
            saveButton.shouldBe(Condition.visible.because("Кнопка «Сохранить» должна быть видима"));
            contentsButton.shouldBe(Condition.visible.because("Кнопка «Содержание» должна быть видима"));
            return this;
        });
    }

    public ArticleScreen tapSave() {
        return step("Нажать «Сохранить» на экране статьи", () -> {
            saveButton
                    .shouldBe(Condition.visible.because("Кнопка «Сохранить» должна быть видима"))
                    .shouldBe(Condition.enabled.because("Кнопка «Сохранить» должна быть доступна"))
                    .shouldHave(Condition.attribute("clickable", "true")
                            .because("Кнопка «Сохранить» должна быть кликабельна"))
                    .tap();
            return this;
        });
    }

    public ArticleScreen openOverflow() {
        return step("Открыть оверфлоу-меню (⋮)", () -> {
            overflowTopButton
                    .shouldBe(Condition.visible.because("Кнопка «Больше настроек» должна быть видима"))
                    .shouldBe(Condition.enabled.because("Кнопка «Больше настроек» должна быть доступна"))
                    .shouldHave(Condition.attribute("clickable", "true")
                            .because("Кнопка «Больше настроек» должна быть кликабельна"))
                    .tap();
            overflow.shouldBeOpen();
            return this;
        });
    }

    public ArticleScreen goToExplore() {
        overflow.goToExplore();
        return this;
    }

}

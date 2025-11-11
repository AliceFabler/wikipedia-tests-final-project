package guru.qa.ui.screens;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import guru.qa.ui.screens.components.ArticleOverflowMenu;
import io.appium.java_client.AppiumBy;
import lombok.extern.slf4j.Slf4j;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;

/**
 * Экран статьи (Article screen).
 *
 * <p><b>Задачи:</b> базовые действия из нижней панели (сохранить/содержание) и работа с оверфлоу-меню.
 * Автодокрутка сюда намеренно не включена — при необходимости выполняется на уровне шагов/утилит.</p>
 */
@Slf4j
public class ArticleScreen {

    /** Кнопка «Сохранить». */
    private final SelenideAppiumElement saveButton =
            $(AppiumBy.id("org.wikipedia.alpha:id/page_save"));
    /** Кнопка «Содержание». */
    private final SelenideAppiumElement contentsButton =
            $(AppiumBy.id("org.wikipedia.alpha:id/page_contents"));
    /** Кнопка «⋮» (верхний тулбар). */
    private final SelenideAppiumElement overflowTopButton =
            $(AppiumBy.id("org.wikipedia.alpha:id/page_toolbar_button_show_overflow_menu"));

    ArticleOverflowMenu overflow = new ArticleOverflowMenu();

    /**
     * Проверить, что экран статьи открыт (видны базовые действия).
     *
     * @return текущий экран
     */
    public ArticleScreen shouldBeOpen() {
        return step("Экран статьи открыт", () -> {
            saveButton.shouldBe(Condition.visible.because("Кнопка «Сохранить» должна быть видима"));
            contentsButton.shouldBe(Condition.visible.because("Кнопка «Содержание» должна быть видима"));
            return this;
        });
    }

    /**
     * Нажать «Сохранить» (выполнить проверку видимости/доступности/кликабельности).
     *
     * @return текущий экран
     */
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

    /**
     * Открыть оверфлоу-меню «⋮» и дождаться его появления.
     *
     * @return текущий экран
     */
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

    /**
     * Перейти в «Ленту / Explore» через оверфлоу-меню.
     *
     * @return текущий экран
     */
    public ArticleScreen goToExplore() {
        overflow.goToExplore();
        return this;
    }
}

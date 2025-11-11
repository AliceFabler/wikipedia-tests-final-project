package guru.qa.ui.screens;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.appium.java_client.AppiumBy;
import lombok.extern.slf4j.Slf4j;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;
import static io.appium.java_client.AppiumBy.id;

/**
 * Экран «Сохранённые» / <b>Saved (Reading lists)</b> — Wikipedia Android.
 *
 * <p><b>Назначение:</b> базовые действия на вкладке Saved:
 * проверка открытия, закрытие стартового промо, вход в дефолтный список «Сохранённое»
 * при необходимости и валидация наличия сохранённой статьи по заголовку.</p>
 *
 * <p><b>Инварианты:</b> Appium 3 + UiAutomator2, только стабильные локаторы (resource-id / accessibilityId),
 * ожидания — через Selenide Conditions, элементы — {@code SelenideAppiumElement}.</p>
 *
 * <p><b>EN:</b> Saved screen Page Object: checks openness, dismisses weekly promo,
 * opens default reading list if present, and asserts a saved article by title.</p>
 */
@Slf4j
public class SavedScreen {

    /** Кнопка поиска/фильтра списков в тулбаре Saved. */
    private final SelenideAppiumElement filterButton =
            $(id("org.wikipedia.alpha:id/menu_search_lists"));

    /** Кнопка оверфлоу (⋮) в тулбаре Saved. */
    private final SelenideAppiumElement overflowButton =
            $(id("org.wikipedia.alpha:id/menu_overflow_button"));

    /** Промо-карточка «Weekly Reading List». */
    private final SelenideAppiumElement onboardingCard =
            $(id("org.wikipedia.alpha:id/onboarding_view"));

    /** Кнопка «Нет, спасибо» в промо. */
    private final SelenideAppiumElement promoDismissBtn =
            $(id("org.wikipedia.alpha:id/negativeButton"));

    /**
     * Проверить, что экран «Сохранённые» открыт (видны ключевые элементы тулбара).
     * <br><b>EN:</b> Assert Saved screen is open (toolbar elements visible).
     *
     * @return текущий экран
     */
    public SavedScreen shouldBeOpen() {
        return step("Экран «Сохранённые» открыт", () -> {
            filterButton.shouldBe(Condition.visible.because("Кнопка фильтра должна быть видима"));
            overflowButton.shouldBe(Condition.visible.because("Кнопка оверфлоу должна быть видима"));
            return this;
        });
    }

    /**
     * Закрыть промо «Weekly Reading List», если оно отображается.
     * <br><b>EN:</b> Dismiss weekly promo if present.
     */
    public void dismissWeeklyPromoIfShown() {
        step("Закрыть промо на вкладке «Сохранённые», если показано", () -> {
            if (onboardingCard.exists()) {
                promoDismissBtn.shouldBe(Condition.visible).click();
            }
        });
    }

    /**
     * Открыть дефолтный список «Сохранённое» (Saved), если на экране показан список списков.
     * <br><b>EN:</b> Enter default “Saved” reading list if a list-of-lists is shown.
     */
    public void openDefaultReadingListIfNeeded() {
        step("Открыть дефолтный список «Сохранённое», если требуется", () -> {
            var defaultList = $(AppiumBy.xpath(
                    "//*[matches(lower-case(@text),'^сохранённое$|^saved$')]"));
            if (defaultList.exists()) {
                defaultList.click();
            }
        });
    }

    /**
     * Проверить, что внутри текущего списка есть статья с указанным заголовком.
     * <br><b>EN:</b> Assert that the current reading list contains an article with the given title.
     *
     * @param data источник данных с именем статьи ({@code getArticleName()})
     */
    public void shouldContainArticleTitled(guru.qa.ui.utils.DataExtractor data) {
        step("Статья из поиска присутствует в списке чтения", () -> {
            final String title = data.getArticleName();
            $(AppiumBy.xpath(
                    "//android.widget.TextView[matches(lower-case(@text),'^" + title.toLowerCase() + "$')]"
            )).shouldBe(Condition.visible.because("Статья '" + title + "' должна быть в списке"));
        });
    }
}

package guru.qa.ui.screens;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.appium.java_client.AppiumBy;
import lombok.extern.slf4j.Slf4j;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;
import static io.appium.java_client.AppiumBy.id;

/*
 * 🎯 MASTER PROMPT (final)
 * SavedScreen (Wikipedia Android → вкладка «Сохранённые») на Appium 3 + UIAutomator2:
 * — Только стабильные локаторы (resource-id/accessibilityId), без UiSelector;
 * — Поля: тулбар (фильтр, оверфлоу), режим фильтра (action mode), пункты оверфлоу,
 *         промо-карточка Weekly Reading List;
 * — Шаги: shouldBeOpen(), openFilter(), typeFilterQuery(String), closeFilterMode(),
 *         openOverflow(), selectOverflow*(), startWeeklyPromo(), dismissWeeklyPromo().
 *
 * Примечание: для прокрутки/поиска по списку чтения добавим отдельный компонент при необходимости.
 */

/**
 * # SavedScreen — экран «Сохранённые» / Reading lists (Saved)
 * <p>
 * Инкапсулирует базовые действия: вход/выход из режима фильтрации списков,
 * работа с оверфлоу-меню и взаимодействие с стартовой промо-карточкой.
 * <p>
 * Инварианты:
 * - Appium 3 + UIAutomator2; ожидания только через Selenide Conditions;
 * - используем SelenideAppiumElement для полей (без SelenideElement).
 */
@Slf4j
public class SavedScreen {

    // ─────────────────────────── Toolbar ───────────────────────────

    /**
     * Кнопка «Отфильтровать мои списки» (лупа в тулбаре Saved).
     */
    private final SelenideAppiumElement filterButton =
            $(id("org.wikipedia.alpha:id/menu_search_lists"));

    /**
     * Кнопка «Больше настроек» (иконка троеточия в тулбаре Saved).
     */
    private final SelenideAppiumElement overflowButton =
            $(id("org.wikipedia.alpha:id/menu_overflow_button"));

    // ─────────────────────── Promo / Onboarding ───────────────────────

    /**
     * Карточка «Еженедельный список для чтения» (если показана).
     */
    private final SelenideAppiumElement onboardingCard =
            $(id("org.wikipedia.alpha:id/onboarding_view"));

    /**
     * Кнопка «Нет, спасибо».
     */
    private final SelenideAppiumElement promoDismissBtn =
            $(id("org.wikipedia.alpha:id/negativeButton"));

    // ───────────────────────────── Steps ─────────────────────────────

    public SavedScreen shouldBeOpen() {
        return step("Экран «Сохранённые» открыт", () -> {
            filterButton.shouldBe(Condition.visible.because("Кнопка фильтра должна быть видима"));
            overflowButton.shouldBe(Condition.visible.because("Кнопка оверфлоу должна быть видима"));
            return this;
        });
    }

    /**
     * Закрыть промо «Еженедельный список...» если показан.
     */
    public void dismissWeeklyPromoIfShown() {
        step("Закрыть промо на вкладке «Сохранённые», если показано", () -> {
            if (onboardingCard.exists()) {
                promoDismissBtn.shouldBe(Condition.visible).click();
            }
        });
    }

    /**
     * Войти в дефолтный список «Сохранённое», если на экране список списков.
     */
    public void openDefaultReadingListIfNeeded() {
        step("Открыть дефолтный список «Сохранённое», если требуется", () -> {
            // типовые id карточки списка: item_title / reading_list_title — не стабильны между билдами,
            // поэтому берём универсальный XPath по тексту «Сохранённое».
            SelenideAppiumElement defaultList = $(AppiumBy.xpath(
                    "//*[matches(lower-case(@text),'^сохранённое$|^saved$')]"));
            if (defaultList.exists()) {
                defaultList.click();
            }
        });
    }

    /**
     * Проверить, что внутри списка есть статья с заголовком из DataExtractor.
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

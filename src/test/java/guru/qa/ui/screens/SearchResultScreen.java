package guru.qa.ui.screens;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumCollection;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import guru.qa.ui.utils.DataExtractor;
import io.appium.java_client.AppiumBy;
import lombok.extern.slf4j.Slf4j;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.appium.SelenideAppium.$;
import static com.codeborne.selenide.appium.SelenideAppium.$$;
import static guru.qa.ui.allure.Steps.step;

/*
 * 🎯 MASTER PROMPT (final)
 * Реализовать экран результатов поиска (Wikipedia Android) на Selenide-Appium:
 *  — Appium 3 + UIAutomator2 (XPath2 по умолчанию в драйвере, но здесь не нужен);
 *  — только устойчивые локаторы: resource-id / accessibilityId; никаких UiSelector;
 *  — поля: список результатов, заголовки, текст пустого состояния, поле ввода, кнопка очистки;
 *  — шаги: shouldBeOpen(), shouldHaveAtLeast(int), openFirstResultAndRememberTitle(DataExtractor),
 *          openByExactTitle(String), openByIndex(int), readAllTitles(), clearQuery(), shouldBeEmptyState();
 *  — логи, because-сообщения, двуязычные @Step.
 */

/**
 * # SearchResultPage — экран результатов поиска Wikipedia (Android)
 */
@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class SearchResultScreen {

    // ────────────────────────────── Elements ──────────────────────────────

    /**
     * RecyclerView со списком результатов (корневой список выдачи).
     */
    private final SelenideAppiumElement resultsList =
            $(AppiumBy.id("org.wikipedia.alpha:id/search_results_list"));

    /**
     * Коллекция заголовков карточек результата (видимая часть списка).
     */
    private final SelenideAppiumCollection resultTitles =
            $$(AppiumBy.id("org.wikipedia.alpha:id/page_list_item_title"));

    /**
     * Поле ввода поискового запроса в тулбаре.
     */
    private final SelenideAppiumElement searchInput =
            $(AppiumBy.id("org.wikipedia.alpha:id/search_src_text"));

    // ────────────────────────────── Steps ──────────────────────────────

    public SearchResultScreen shouldBeOpen() {
        return step("Экран результатов открыт / Results screen is open", () -> {
            resultsList
                    .should(Condition.exist.because("Список результатов должен существовать"))
                    .should(Condition.visible.because("Список результатов должен быть видим"));
            return this;
        });
    }

    public SearchResultScreen shouldHaveAtLeast(int min) {
        return step("Проверить, что результатов не меньше " + min, () -> {
            resultTitles.shouldHave(sizeGreaterThan(min - 1)
                    .because("Ожидаем минимум " + min + " результатов поиска"));
            log.info("Найдено результатов ≥ {}", min);
            return this;
        });
    }

    public void openFirstResultAndRememberTitle(final DataExtractor extractor) {
        step("Открыть первый результат и запомнить заголовок", () -> {
            shouldBeOpen().shouldHaveAtLeast(1);
            final SelenideAppiumElement first = resultTitles.first();
            final String title = first.getText().trim();
            log.info("Первый результат: '{}'", title);
            extractor.setArticleName(title);
            first.click();
        });
    }

    public SearchResultScreen typeQuery(String query) {
        return step("Ввести поисковый запрос: " + query, () -> {
            searchInput.shouldBe(Condition.visible
                    .because("Поле поиска должно быть видно")).clear();
            searchInput.setValue(query);
            resultsList.should(Condition.exist.because("Должен появиться список результатов"));
            return this;
        });
    }
}

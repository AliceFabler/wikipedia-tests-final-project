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

/**
 * Экран результатов поиска / <b>Search results screen</b> (Wikipedia Android).
 *
 * <p><b>Что умеет:</b>
 * проверка открытия списка, валидация минимального числа результатов,
 * открытие первого результата с сохранением его заголовка, ввод запроса.</p>
 *
 * <p><b>Инварианты:</b> Appium 3 + UiAutomator2; только стабильные локаторы (resource-id/accessibilityId);
 * элементы — {@code SelenideAppiumElement}/{@code SelenideAppiumCollection}; без UiSelector.</p>
 *
 * <p><b>EN:</b> Page Object for search results: asserts openness, checks result count,
 * opens the first result and remembers its title, types a query. Uses stable ids only.</p>
 */
@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class SearchResultScreen {

    private final SelenideAppiumElement resultsList =
            $(AppiumBy.id("org.wikipedia.alpha:id/search_results_list"));

    private final SelenideAppiumCollection resultTitles =
            $$(AppiumBy.id("org.wikipedia.alpha:id/page_list_item_title"));

    private final SelenideAppiumElement searchInput =
            $(AppiumBy.id("org.wikipedia.alpha:id/search_src_text"));

    /**
     * Экран результатов открыт (список существует и видим).
     * <br><b>EN:</b> Results list exists and is visible.
     */
    public SearchResultScreen shouldBeOpen() {
        return step("Экран результатов открыт / Results screen is open", () -> {
            resultsList
                    .should(Condition.exist.because("Список результатов должен существовать"))
                    .should(Condition.visible.because("Список результатов должен быть видим"));
            return this;
        });
    }

    /**
     * Проверить, что найдено не меньше {@code min} результатов.
     * <br><b>EN:</b> Assert there are at least {@code min} results.
     *
     * @param min минимально ожидаемое количество
     */
    public SearchResultScreen shouldHaveAtLeast(int min) {
        return step("Проверить, что результатов не меньше " + min, () -> {
            resultTitles.shouldHave(sizeGreaterThan(min - 1)
                    .because("Ожидаем минимум " + min + " результатов поиска"));
            log.info("Найдено результатов ≥ {}", min);
            return this;
        });
    }

    /**
     * Открыть первый результат и сохранить его заголовок в {@code extractor}.
     * <br><b>EN:</b> Open the first result and store its title to {@code extractor}.
     *
     * @param extractor приёмник данных (см. {@link DataExtractor})
     */
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

    /**
     * Ввести поисковый запрос в поле поиска.
     * <br><b>EN:</b> Type query into the search input.
     *
     * @param query строка запроса
     * @return текущий экран
     */
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

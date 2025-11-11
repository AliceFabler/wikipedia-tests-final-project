package guru.qa.ui.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import static io.qameta.allure.Allure.step;

/**
 * Тесты поиска (Wikipedia Android) — мануальные сценарии со Step-логами.
 *
 * <p><b>Назначение:</b> шаблонные кейсы для ручной проверки поведения поиска:
 * релевантность, пустая выдача, пустой запрос, ручной ввод, фильтры и обработка
 * некорректного ввода. Каждый тест оформлен в виде последовательности
 * {@code Allure.step(...)} без реальной автоматизации действий.</p>
 *
 * <p><b>Инварианты:</b> метки Allure/Tags используются для классификации; шаги —
 * только через {@code Allure.step}; без привязки к Page Object’ам.</p>
 *
 * <p><b>EN:</b> Manual search test scenarios with Allure steps only. Intended for
 * exploratory/hand-run verification of relevance, empty results, empty query,
 * manual input, filters, and invalid input handling.</p>
 */
@Epic("Wikipedia (Android)")
@Feature("Search / Поиск")
@Owner("Alice Fabler")
@Tags({@Tag("android"), @Tag("manual"), @Tag("wikipedia")})
@Severity(SeverityLevel.MINOR)
public class SearchTests {

    /**
     * Случайный запрос должен возвращать непустые и релевантные результаты.
     * <br><b>EN:</b> Random query returns non-empty, relevant results.
     */
    @Test
    @DisplayName("Поиск: случайный запрос возвращает релевантные результаты")
    @Story("Поиск в приложении Wikipedia")
    @Description("""
            Открыть главную страницу → сгенерировать случайный запрос → ввести запрос в поиск →
            убедиться, что результаты не пусты и первый результат содержит поисковое слово.
            """)
    @Issue("HOMEWORK-1530")
    void successfulSearchRemoteTest() {
        step("Открыть главную страницу приложения");
        step("Генерировать случайный запрос для поиска");
        step("Ввести запрос в поле поиска");
        step("Проверить, что результаты поиска не пусты");
        step("Проверить, что первый результат содержит поисковое слово");
    }

    /**
     * Несуществующий запрос должен приводить к пустой выдаче.
     * <br><b>EN:</b> Non-existent query yields no results.
     */
    @Test
    @DisplayName("Поиск: несуществующий запрос даёт пустой результат")
    @Story("Поиск в приложении Wikipedia")
    @Description("""
            Открыть главную страницу → ввести заведомо несуществующий запрос →
            убедиться, что список результатов пуст.
            """)
    @Issue("HOMEWORK-1531")
    void searchNoResultsTest() {
        step("Открыть главную страницу приложения");
        step("Ввести несуществующий запрос в поле поиска");
        step("Проверить, что результаты поиска пусты");
    }

    /**
     * Пустой запрос не должен отображать результаты.
     * <br><b>EN:</b> Empty query should not show results.
     */
    @Test
    @DisplayName("Поиск: пустой запрос не должен показывать результаты")
    @Story("Поиск в приложении Wikipedia")
    @Description("""
            Открыть главную страницу → оставить поле поиска пустым →
            убедиться, что результаты не отображаются.
            """)
    @Issue("HOMEWORK-1532")
    void emptySearchQueryTest() {
        step("Открыть главную страницу приложения");
        step("Не вводить запрос в поле поиска");
        step("Проверить, что результаты поиска пусты");
    }

    /**
     * Ручной ввод запроса — результаты должны соответствовать запросу.
     * <br><b>EN:</b> Manual query input yields matching results.
     */
    @Test
    @DisplayName("Поиск: ручной ввод запроса и проверка релевантности")
    @Story("Поиск в приложении Wikipedia")
    @Description("""
            Открыть страницу поиска → ввести произвольный запрос →
            убедиться, что результаты соответствуют запросу.
            """)
    @Issue("HOMEWORK-1533")
    void manualSearchTest() {
        step("Открыть страницу поиска");
        step("Ввести запрос");
        step("Проверить, что результаты поиска соответствуют запросу");
    }

    /**
     * Изменение параметров фильтра должно менять выдачу.
     * <br><b>EN:</b> Changing filters updates the results list.
     */
    @Test
    @DisplayName("Поиск: изменение параметров фильтрации отражается в результатах")
    @Story("Поиск в приложении Wikipedia")
    @Description("""
            Открыть страницу поиска → изменить параметры фильтрации →
            убедиться, что выдача изменилась согласно фильтрам.
            """)
    @Issue("HOMEWORK-1534")
    void filterSearchTest() {
        step("Открыть страницу поиска");
        step("Изменить фильтры поиска");
        step("Проверить, что фильтрация работает корректно");
    }

    /**
     * Некорректный ввод должен сопровождаться сообщением об ошибке.
     * <br><b>EN:</b> Invalid input shows an error message.
     */
    @Test
    @DisplayName("Поиск: некорректный ввод показывает сообщение об ошибке")
    @Story("Поиск в приложении Wikipedia")
    @Description("""
            Открыть страницу поиска → ввести некорректный запрос →
            убедиться, что отображается сообщение об ошибке.
            """)
    @Issue("HOMEWORK-1535")
    void invalidSearchTest() {
        step("Открыть страницу поиска");
        step("Ввести некорректный запрос");
        step("Проверить, что отображается сообщение об ошибке");
    }
}

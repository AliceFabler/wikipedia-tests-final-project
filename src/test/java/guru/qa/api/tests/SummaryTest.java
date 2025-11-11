package guru.qa.api.tests;

import guru.qa.api.WikipediaApi;
import guru.qa.api.models.PageSummary;
import io.qameta.allure.*;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Позитивные проверки эндпоинта {@code GET /page/summary/{title}}.
 *
 * <p>Содержит:
 * <ul>
 *   <li>Смоук-набор для фиксированных статей;</li>
 *   <li>Параметризованный набор для случайного подмножества популярных страниц
 *   с детерминированным перемешиванием на основе {@link Faker}.</li>
 * </ul>
 */
@Epic("Wikipedia REST API")
@Feature("Page Summary")
@Owner("Alice Fabler")
@Tags({@Tag("api"), @Tag("wikipedia"), @Tag("rest")})
@Link(name = "REST docs", url = "https://en.wikipedia.org/api/rest_v1/")
@DisplayName("Wikipedia REST: /page/summary — позитивные проверки")
public class SummaryTest {

    private final WikipediaApi api = new WikipediaApi();

    /**
     * Смоук-проверка сводки для фиксированного набора страниц.
     *
     * @param title заголовок статьи (пример: {@code Sweden})
     * @param lang  язык ответа (пример: {@code en})
     */
    @ParameterizedTest(name = "Сводка для статьи \"{0}\" → 200 JSON, lang={1}")
    @CsvSource({
            "Sweden,en",
            "Stockholm,en"
    })
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REST /page/summary: сводка по фиксированным страницам (EN)")
    @Description("""
            GET /page/summary/{title} (lang={1}) для белого списка статей →
            проверяем: 200 + нормализованный заголовок + язык ответа + непустой extract.
            """)
    @Issue("HOMEWORK-1536")
    @AllureId("40943")
    void pageSummary_smoke(String title, String lang) {
        PageSummary summary = api.getPageSummary(title, lang);

        assertThat(summary).as("Ответ десериализован").isNotNull();
        assertThat(summary.getTitle()).as("Заголовок").isEqualTo(normalizeTitle(title));
        assertThat(summary.getLanguage()).as("Язык ответа").isEqualTo(lang);
        assertThat(summary.getExtract()).as("Краткое описание не пустое").isNotBlank();
    }

    /**
     * Источник параметров: случайное подмножество популярных страниц
     * с детерминированным перемешиванием на основе {@link Faker}.
     *
     * @return поток аргументов (title, lang)
     */
    static Stream<Arguments> randomPopularPages() {
        Faker faker = new Faker();
        List<String> whitelist = List.of(
                "Sweden",
                "Stockholm",
                "Germany",
                "France",
                "Japan",
                "Java_(programming_language)",
                "Appium",
                "Selenium_(software)"
        );
        long seed = faker.random().nextLong();
        List<String> shuffled = new ArrayList<>(whitelist);
        Collections.shuffle(shuffled, new Random(seed));
        return shuffled.stream().limit(3).map(t -> arguments(t, "en"));
    }

    /**
     * Динамическая проверка сводки для случайного поднабора whitelist.
     *
     * @param title заголовок статьи
     * @param lang  язык ответа
     */
    @ParameterizedTest(name = "Dynamic: сводка для \"{0}\" → 200 JSON, lang={1}")
    @MethodSource("randomPopularPages")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REST /page/summary: сводка по случайному подмножеству популярных страниц (EN)")
    @Description("""
            GET /page/summary/{title} (lang={1}) →
            проверяем: 200 + нормализованный заголовок + язык ответа + непустой extract.
            """)
    @Issue("HOMEWORK-1537")
    @AllureId("40944")
    void pageSummary_dynamic(String title, String lang) {
        PageSummary summary = api.getPageSummary(title, lang);

        assertThat(summary).as("Ответ десериализован").isNotNull();
        assertThat(summary.getTitle()).as("Заголовок").isEqualTo(normalizeTitle(title));
        assertThat(summary.getLanguage()).as("Язык ответа").isEqualTo(lang);
        assertThat(summary.getExtract()).as("Краткое описание не пустое").isNotBlank();
    }

    /**
     * Нормализация заголовка: декодирование URL и замена подчёркиваний на пробелы.
     *
     * @param raw исходное значение
     * @return нормализованная строка
     */
    private static String normalizeTitle(String raw) {
        try {
            String decoded = java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8);
            return decoded.replace('_', ' ').trim();
        } catch (Exception e) {
            return raw.replace('_', ' ').trim();
        }
    }
}

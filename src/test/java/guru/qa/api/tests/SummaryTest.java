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
 * Параметризованные примеры для Wikipedia REST /page/summary/{title}
 * Используем DataFaker для рандомизации отбора страниц из белого списка.
 */
@Epic("Wikipedia REST API")
@Feature("Page Summary")
@Owner("Alice Fabler")
@Tags({@Tag("api"), @Tag("wikipedia"), @Tag("rest")})
@Link(name = "REST docs", url = "https://en.wikipedia.org/api/rest_v1/")
@DisplayName("Wikipedia REST: /page/summary — позитивные проверки")
public class SummaryTest {

    private final WikipediaApi api = new WikipediaApi();

    @ParameterizedTest(name = "Сводка для статьи \"{0}\" → 200 JSON, lang={1}")
    @CsvSource({
            "Sweden,en",
            "Stockholm,en"
    })
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REST /page/summary: сводка по фиксированным страницам (EN)")
    @Description("""
            GET /page/summary/{title} (lang={1}) для белого списка статей →
            проверяем: 200 + корректный заголовок (нормализованный) + язык ответа и непустой extract.
            """)
    @Issue("HOMEWORK-1536")
//    @TmsLink("TMS-API-PS-001")
//    @AllureId("API-PS-001")
    void pageSummary_smoke(String title, String lang) {
        PageSummary summary = api.getPageSummary(title, lang);

        assertThat(summary).as("Ответ десериализован").isNotNull();
        assertThat(summary.getTitle()).as("Заголовок").isEqualTo(normalizeTitle(title));
        assertThat(summary.getLanguage()).as("Язык ответа").isEqualTo(lang);
        assertThat(summary.getExtract()).as("Краткое описание не пустое").isNotBlank();
    }

    /** Источник параметров с Faker: случайный порядок из белого списка популярных статей */
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
        // Перемешаем детерминированно на основе Faker, чтобы прогон был воспроизводимым в рамках одного запуска
        long seed = faker.random().nextLong();
        List<String> shuffled = new ArrayList<>(whitelist);
        Collections.shuffle(shuffled, new Random(seed));
        return shuffled.stream().limit(3).map(t -> arguments(t, "en"));
    }

    @ParameterizedTest(name = "Dynamic: сводка для \"{0}\" → 200 JSON, lang={1}")
    @MethodSource("randomPopularPages")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REST /page/summary: сводка по случайному подмножеству популярных страниц (EN)")
    @Description("""
            GET /page/summary/{title} (lang={1}) для случайного поднабора whitelist →
            воспроизводимое перемешивание, проверяем 200 + заголовок (нормализованный) + язык и непустой extract.
            """)
    @Issue("HOMEWORK-1537")
//    @TmsLink("TMS-API-PS-002")
//    @AllureId("API-PS-002")
    void pageSummary_dynamic(String title, String lang) {
        PageSummary summary = api.getPageSummary(title, lang);

        assertThat(summary).as("Ответ десериализован").isNotNull();
        assertThat(summary.getTitle()).as("Заголовок").isEqualTo(normalizeTitle(title));
        assertThat(summary.getLanguage()).as("Язык ответа").isEqualTo(lang);
        assertThat(summary.getExtract()).as("Краткое описание не пустое").isNotBlank();
    }

    private static String normalizeTitle(String raw) {
        try {
            String decoded = java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8);
            return decoded.replace('_', ' ').trim();
        } catch (Exception e) {
            return raw.replace('_', ' ').trim();
        }
    }
}

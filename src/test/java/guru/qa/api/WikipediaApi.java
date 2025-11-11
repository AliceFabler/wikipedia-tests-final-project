package guru.qa.api;

import guru.qa.api.models.PageSummary;
import guru.qa.api.spec.ApiSpecs;

import static io.restassured.RestAssured.given;

/**
 * Тонкая обёртка над Rest-Assured для Wikimedia REST API.
 *
 * <p>Содержит методы вызова эндпоинтов и десериализации ответов в модели.
 */
public class WikipediaApi {

    /**
     * Получить краткое описание статьи.
     *
     * @param title заголовок статьи (например, {@code "Sweden"})
     * @param lang  язык (например, {@code "en"})
     * @return десериализованная модель {@link PageSummary}
     */
    public PageSummary getPageSummary(String title, String lang) {
        return given()
                .spec(ApiSpecs.wikiRequestSpec())
                .header("Accept-Language", lang)
                .pathParam("title", title)
                .when()
                .get("/page/summary/{title}")
                .then()
                .spec(ApiSpecs.okJsonSpec())
                .extract().as(PageSummary.class);
    }
}

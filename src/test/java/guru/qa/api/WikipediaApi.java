package guru.qa.api;

import guru.qa.api.models.PageSummary;
import guru.qa.api.spec.ApiSpecs;

import static io.restassured.RestAssured.given;

/**
 * Небольшой слой API поверх Rest-Assured.
 */
public class WikipediaApi {

    /**
     * Получить краткое описание статьи.
     * @param title заголовок статьи (например, "Sweden")
     * @param lang  язык (например, "en")
     * @return десериализованная модель PageSummary
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

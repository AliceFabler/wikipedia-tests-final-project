package guru.qa.api.spec;

import guru.qa.api.config.ApiConfig;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.aeonbits.owner.ConfigFactory;

/**
 * Единая фабрика спецификаций для Rest-Assured.
 *
 * <p>Содержит:
 * <ul>
 *   <li>Запросную спецификацию с базовым URI/путём, логированием и Allure-интеграцией;</li>
 *   <li>Спецификацию успешного JSON-ответа.</li>
 * </ul>
 */
public final class ApiSpecs {

    private ApiSpecs() {}

    /** Конфигурация API, считываемая через Owner. */
    public static final ApiConfig cfg =
            ConfigFactory.create(ApiConfig.class, System.getProperties());

    /**
     * Спецификация запроса к Wikimedia REST API.
     *
     * @return преднастроенная {@link RequestSpecification}
     */
    public static RequestSpecification wikiRequestSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(cfg.baseUrl())
                .setBasePath("/api/rest_v1")
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .addHeader("User-Agent", cfg.userAgent())
                .log(LogDetail.URI)
                .log(LogDetail.METHOD)
                .build();
    }

    /**
     * Спецификация успешного JSON-ответа (HTTP 200).
     *
     * @return преднастроенная {@link ResponseSpecification}
     */
    public static ResponseSpecification okJsonSpec() {
        return new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();
    }
}

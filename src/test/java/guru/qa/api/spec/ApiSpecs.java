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
 * Request/Response спецификации для API-тестов.
 * Единая точка для логирования и Allure-интеграции.
 */
public final class ApiSpecs {

    private ApiSpecs() {}

    public static final ApiConfig cfg =
            ConfigFactory.create(ApiConfig.class, System.getProperties());

    public static RequestSpecification wikiRequestSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(cfg.baseUrl())
                .setBasePath("/api/rest_v1")
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                // Логи запроса/ответа сразу видны в Allure
                .addFilter(new AllureRestAssured())
                // Wikimedia требует «человеческий» UA
                .addHeader("User-Agent", cfg.userAgent())
                .log(LogDetail.URI)
                .log(LogDetail.METHOD)
                .build();
    }

    public static ResponseSpecification okJsonSpec() {
        return new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();
    }
}

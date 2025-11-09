package guru.qa.api.config;

import org.aeonbits.owner.Config;

/**
 * Простая конфигурация API через Owner.
 * Источники: system properties -> env -> classpath:api.properties
 */
@Config.Sources({
        "system:properties",
        "system:env",
        "classpath:api.properties"
})
public interface ApiConfig extends Config {

    @Key("api.baseUrl")
    @DefaultValue("https://en.wikipedia.org")
    String baseUrl();

    @Key("api.lang")
    @DefaultValue("en")
    String lang();

    @Key("api.userAgent")
    @DefaultValue("QA-Guru-Autotests/1.0 (https://example.org; contact@example.org)")
    String userAgent();
}

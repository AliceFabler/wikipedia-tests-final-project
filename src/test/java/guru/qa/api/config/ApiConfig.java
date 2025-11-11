package guru.qa.api.config;

import org.aeonbits.owner.Config;

/**
 * Конфигурация REST-API на базе Owner.
 *
 * <p>Приоритет источников настроек:
 * <ol>
 *   <li>System properties</li>
 *   <li>Переменные окружения</li>
 *   <li>Файл classpath: {@code api.properties}</li>
 * </ol>
 *
 * <p>Примеры переопределений:
 * <pre>
 *   -Dapi.baseUrl=<a href="https://en.wikipedia.org">...</a>
 *   -Dapi.lang=en
 *   -Dapi.userAgent="QA-Guru-Autotests/1.0 (https://example.org; contact@example.org)"
 * </pre>
 */
@Config.Sources({
        "system:properties",
        "system:env",
        "classpath:api.properties"
})
public interface ApiConfig extends Config {

    /** Базовый URL Wikimedia REST API (без суффикса {@code /api/rest_v1}). */
    @Key("api.baseUrl")
    @DefaultValue("https://en.wikipedia.org")
    String baseUrl();

    /** Язык по умолчанию для заголовка {@code Accept-Language}. */
    @Key("api.lang")
    @DefaultValue("en")
    String lang();

    /**
     * Пользовательский User-Agent, соответствующий правилам Wikimedia.
     * <p>Рекомендуется указывать проект и контакт.
     */
    @Key("api.userAgent")
    @DefaultValue("QA-Guru-Autotests/1.0 (https://example.org; contact@example.org)")
    String userAgent();
}

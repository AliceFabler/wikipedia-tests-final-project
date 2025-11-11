package guru.qa.ui.config;

import org.aeonbits.owner.Config;

/**
 * Учетные данные и удалённый URL для подключения к облачному хабу (например, BrowserStack).
 *
 * <p><b>Источник настроек (MERGE):</b> system properties → ENV → {@code classpath:${env}.properties} → {@code classpath:auth.properties}.</p>
 *
 * <p><b>Примеры переопределений:</b>
 * <pre>
 *   -DuserName=alice -Dkey=****** -DremoteUrl=https://hub.browserstack.com/wd/hub
 *   USERNAME=alice KEY=****** REMOTEURL=https://hub.browserstack.com/wd/hub
 * </pre>
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
        "system:properties",
        "system:env",
        "classpath:${env}.properties",
        "classpath:auth.properties"
})
public interface AuthConfig extends Config {

    /** Имя пользователя в облачном провайдере. */
    @Key("userName")
    @DefaultValue("lissarider1")
    String getUserName();

    /** Секретный ключ/токен доступа. */
    @Key("key")
    @DefaultValue("zb7dtizcYQAAoSxS9mKv")
    String getKey();

    /** URL удалённого хаба Appium/WebDriver. */
    @Key("remoteUrl")
    @DefaultValue("http://hub.browserstack.com/wd/hub")
    String getRemoteUrl();
}

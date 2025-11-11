package guru.qa.ui.config;

import org.aeonbits.owner.Config;

/**
 * Удалённая конфигурация запуска (например, BrowserStack/облачный грид).
 *
 * <p><b>Источник настроек (MERGE):</b> system properties → ENV → {@code classpath:${env}.properties} → {@code classpath:remote.properties}.</p>
 *
 * <p>Поддерживает указание приложения, целевого устройства/ОС, метаданных сессии и параметров провайдера.</p>
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
        "system:properties",
        "system:env",
        "classpath:${env}.properties",
        "classpath:remote.properties"
})
public interface RemoteConfig extends Config {

    /** Идентификатор/URL приложения: {@code bs://<app-id>} или публичный HTTP(S)-URL. */
    @Key("app")
    @DefaultValue("bs://sample.app")
    String getApp();

    /** Целевое устройство (пример: {@code Google Pixel 7}). */
    @Key("device")
    @DefaultValue("Google Pixel 7")
    String getDevice();

    /** Версия ОС устройства (пример: {@code 13.0}). */
    @Key("os_version")
    @DefaultValue("13.0")
    String getOsVersion();

    /** Название проекта (отображается в провайдере). */
    @Key("project")
    @DefaultValue("Wikipedia Mobile")
    String getProject();

    /** Название сборки/билда. Поддерживаются переменные окружения CI. */
    @Key("build")
    @DefaultValue("CI ${env.JOB_NAME} #${env.BUILD_NUMBER}")
    String getBuild();

    /** Имя сессии. Поддерживаются переменные окружения CI. */
    @Key("name")
    @DefaultValue("UI ${env.JOB_NAME} #${env.BUILD_NUMBER}")
    String getSessionName();

    /** Версия Appium на стороне провайдера. */
    @Key("appiumVersion")
    @DefaultValue("2.19.0")
    String getAppiumVersion();

    /** Включить расширенный debug у провайдера. */
    @Key("debug")
    @DefaultValue("true")
    String getDebug();

    /** Включить запись видео у провайдера. */
    @Key("video")
    @DefaultValue("true")
    String getVideo();

    /** Значение для {@code appium:platformName} (если требуется прокинуть явно). */
    @Key("platform")
    @DefaultValue("android")
    String getPlatform();

    /** Значение для {@code appium:automationName} (если требуется прокинуть явно). */
    @Key("automation")
    @DefaultValue("uiautomator2")
    String getAutomation();
}

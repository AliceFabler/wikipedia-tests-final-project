package guru.qa.ui.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
        "system:properties",
        "system:env",
        "classpath:${env}.properties",
        "classpath:remote.properties"
})
public interface RemoteConfig extends Config {

    // ---- App under test ----
    /** bs://<app-id> или публичный URL на APK/IPA */
    @Key("app")
    @DefaultValue("bs://sample.app")
    String getApp();

    // ---- Target device ----
    /** Например: Google Pixel 7 */
    @Key("device")
    @DefaultValue("Google Pixel 7")
    String getDevice();

    /** Например: 13.0 */
    @Key("os_version")
    @DefaultValue("13.0")
    String getOsVersion();

    // ---- Session meta (используется для bstack:options) ----
    @Key("project")
    @DefaultValue("Wikipedia Mobile")
    String getProject();

    @Key("build")
    @DefaultValue("CI ${env.JOB_NAME} #${env.BUILD_NUMBER}")
    String getBuild();

    @Key("name")
    @DefaultValue("UI ${env.JOB_NAME} #${env.BUILD_NUMBER}")
    String getSessionName();

    // ---- BrowserStack / Appium настройки ----
    /** Версия Appium на стороне BrowserStack */
    @Key("appiumVersion")
    @DefaultValue("2.19.0")
    String getAppiumVersion();

//    /** Включить сетевые логи в BrowserStack (bstack:options.networkLogs) */
//    @Key("networkLogs")
//    @DefaultValue("true")
//    String getNetworkLogs();

    /** Включить debug на BrowserStack (bstack:options.debug) */
    @Key("debug")
    @DefaultValue("true")
    String getDebug();

    /** Включить видео на BrowserStack (bstack:options.video) */
    @Key("video")
    @DefaultValue("true")
    String getVideo();

    // ---- (опционально) для заполнения appium:* если понадобится из конфигов ----
    /** Прокидывается как appium:platformName (дефолт в драйвере — Android) */
    @Key("platform")
    @DefaultValue("android")
    String getPlatform();

    /** Прокидывается как appium:automationName (дефолт в драйвере — UiAutomator2) */
    @Key("automation")
    @DefaultValue("uiautomator2")
    String getAutomation();
}

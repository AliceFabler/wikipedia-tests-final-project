package guru.qa.ui.config;

import org.aeonbits.owner.Config;

/**
 * Unified source of configuration.
 * Priority: System properties (-Dkey=...), ENV (KEY=...), classpath:${env}.properties, classpath:local.properties.
 * ENV mapping works as you'd expect: "appium.server.url" ⇔ "APPIUM_SERVER_URL", "app.dir" ⇔ "APP_DIR", etc.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
        "system:properties",
        "system:env",
        "classpath:${env}.properties",
        "classpath:local.properties"
})
public interface LocalConfig extends Config {

    // --- Device / platform ---
    @Key("platformVersion")
    String getPlatformVersion();

    @Key("deviceName")
    String getDeviceName();

    @Key("appPackage")
    String getAppPackage();

    @Key("appActivity")
    String getAppActivity();

    // --- Appium server ---
    @Key("appium.server.url")
    @DefaultValue("http://127.0.0.1:4723/wd/hub")
    String getAppiumServerUrl();

    // --- Unified APK pointer ---
    // Single key that may be: absolute/relative file path OR HTTP(S) URL.
    // Examples:
    //   -Dapp=/abs/path/app.apk
    //   -Dapp=https://example.com/app.apk
    //   APP=/abs/path/app.apk
    //   app=... (in local.properties / remote.properties)
    @Key("app")
    @DefaultValue("")
    String getApp();

    // --- APK folder (where we cache/download APKs if "app" is URL or empty) ---
    @Key("app.dir")
    @DefaultValue("src/test/resources/apps")
    String getAppDir();

    // --- APK filename (used when "app" is empty or URL has no filename) ---
    @Key("app.filename")
    @DefaultValue("app-alpha-universal-release.apk")
    String getAppFilename();

    // --- Default APK URL (used when "app" is empty) ---
    @Key("app.url")
    @DefaultValue("https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/app-alpha-universal-release.apk")
    String getAppUrl();
}

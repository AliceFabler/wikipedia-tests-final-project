package guru.qa.ui.config;

import org.aeonbits.owner.Config;

/**
 * Локальная конфигурация запуска: устройство/платформа, адрес Appium, артефакты приложения.
 *
 * <p><b>Источник настроек (MERGE):</b> system properties → ENV → {@code classpath:${env}.properties} → {@code classpath:local.properties}.</p>
 *
 * <p><b>ENV-маппинг:</b> ключ {@code appium.server.url} ⇔ переменная {@code APPIUM_SERVER_URL}, {@code app.dir} ⇔ {@code APP_DIR} и т.д.</p>
 *
 * <p><b>Маршрутизация APK:</b> {@code app} может быть путём к файлу или HTTP(S)-URL. Если пусто — берётся {@code app.url},
 * скачивается в {@code app.dir} c именем {@code app.filename}.</p>
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
        "system:properties",
        "system:env",
        "classpath:${env}.properties",
        "classpath:local.properties"
})
public interface LocalConfig extends Config {

    /** Версия платформы устройства (например, {@code 14}). */
    @Key("platformVersion")
    String getPlatformVersion();

    /** Имя/модель локального девайса/эмулятора. */
    @Key("deviceName")
    String getDeviceName();

    /** Android applicationId (package). */
    @Key("appPackage")
    String getAppPackage();

    /** Android launcher activity. */
    @Key("appActivity")
    String getAppActivity();

    /** Адрес локального Appium сервера. */
    @Key("appium.server.url")
    @DefaultValue("http://127.0.0.1:4723/wd/hub")
    String getAppiumServerUrl();

    /** Унифицированный указатель на приложение: путь к APK/IPA или HTTP(S)-URL. */
    @Key("app")
    @DefaultValue("")
    String getApp();

    /** Директория для хранения/кеша APK. */
    @Key("app.dir")
    @DefaultValue("src/test/resources/apps")
    String getAppDir();

    /** Имя файла APK по умолчанию (если URL без имени или {@code app} пуст). */
    @Key("app.filename")
    @DefaultValue("app-alpha-universal-release.apk")
    String getAppFilename();

    /** URL APK по умолчанию для автозагрузки при пустом {@code app}. */
    @Key("app.url")
    @DefaultValue("https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/app-alpha-universal-release.apk")
    String getAppUrl();
}

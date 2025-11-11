package guru.qa.ui.drivers;

import com.codeborne.selenide.WebDriverProvider;
import guru.qa.ui.config.LocalConfig;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import lombok.extern.slf4j.Slf4j;
import org.aeonbits.owner.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import static io.appium.java_client.remote.AutomationName.ANDROID_UIAUTOMATOR2;
import static io.appium.java_client.remote.MobilePlatform.ANDROID;

/**
 * Локальный провайдер {@link WebDriver} для Android на базе Appium (UiAutomator2).
 *
 * <p><b>Особенности:</b>
 * <ul>
 *   <li>Конфигурация через {@link LocalConfig} (MERGE: system props → env → {@code ${env}.properties} → {@code local.properties});</li>
 *   <li>Проверка доступности Appium по {@code /status} с дружелюбными подсказками к {@code base-path};</li>
 *   <li>Унифицированное разрешение приложения: путь/URL/автозагрузка с атомарной записью;</li>
 *   <li>Явные опции {@link UiAutomator2Options} и развёрнутая диагностика ошибок старта.</li>
 * </ul>
 *
 * <p><b>Минимальные требования:</b> запущенный Appium Server и доступный устройства/эмулятор.</p>
 */
@Slf4j
public final class LocalDriver implements WebDriverProvider {

    /**
     * Быстро проверяет, что Appium отвечает по {@code /status}, учитывая возможный {@code /wd/hub} base-path.
     *
     * @param serverUrl адрес Appium
     * @throws IllegalStateException если сервер недоступен или отвечает некорректно
     */
    @SuppressWarnings("resource")
    private static void ensureAppiumAlive(URL serverUrl) {
        try {
            var client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            String base = serverUrl.toString().replaceAll("/+$", "");
            var candidates = new String[]{base + "/status", base.replace("/wd/hub", "") + "/status"};

            boolean ok = false;
            for (String s : candidates) {
                try {
                    var req = HttpRequest.newBuilder(URI.create(s))
                            .timeout(Duration.ofSeconds(3))
                            .GET()
                            .build();
                    var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() / 100 == 2 || resp.statusCode() == 404) {
                        ok = true;
                        break;
                    }
                } catch (Exception ignore) { }
            }
            if (!ok) throw new IllegalStateException("No response from Appium");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Appium is not reachable at " + serverUrl + "\n" +
                            "Start it locally:\n" +
                            "  • если в URL есть '/wd/hub':  appium --base-path /wd/hub\n" +
                            "  • иначе (base-path '/'):       appium\n" +
                            "Либо переопредели URL: -Dappium.server.url=http://127.0.0.1:4723/\n", e);
        }
    }

    /* ===================== App path resolution (Owner only) ===================== */

    /**
     * Преобразует строку в {@link URL} с понятной диагностикой.
     *
     * @param raw         исходная строка
     * @param keyForError имя ключа для сообщения об ошибке
     * @return валидный URL
     * @throws IllegalArgumentException при неверном формате URL
     */
    @SuppressWarnings("SameParameterValue")
    private static URL toUrl(String raw, String keyForError) {
        try {
            return URI.create(raw).toURL();
        } catch (MalformedURLException e) {
            log.error("Invalid {}='{}'. Cause: {}", keyForError, raw, e, e);
            throw new IllegalArgumentException("Bad " + keyForError + ": " + raw, e);
        }
    }

    /* =============================== Helpers =============================== */

    /**
     * Проверяет, является ли строка HTTP(S)-URL.
     *
     * @param s строка
     * @return {@code true}, если начинается с http:// или https://
     */
    private static boolean isHttp(String s) {
        String x = s.toLowerCase();
        return x.startsWith("http://") || x.startsWith("https://");
    }

    /**
     * Обрезает пробелы и превращает пустую строку в {@code null}.
     *
     * @param s исходная строка
     * @return {@code null} или обрезанное значение
     */
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Извлекает имя файла из URL или возвращает значение по умолчанию.
     *
     * @param url исходный URL
     * @param cfg конфигурация с дефолтным именем
     * @return имя файла
     */
    private static String fileNameOrDefault(String url, LocalConfig cfg) {
        try {
            String path = URI.create(url).getPath();
            int idx = path.lastIndexOf('/');
            String name = (idx >= 0 ? path.substring(idx + 1) : path);
            String fallback = cfg.getAppFilename();
            String candidate = trimToNull(name);
            return candidate != null ? candidate : fallback;
        } catch (Exception ignore) {
            return cfg.getAppFilename();
        }
    }

    /**
     * Атомарная загрузка файла: скачивает во временный {@code *.part} и переименовывает.
     *
     * @param url    источник
     * @param target путь сохранения
     * @throws Exception при сетевых/IO-ошибках или неуспешном ответе
     */
    @SuppressWarnings("resource")
    private static void download(String url, Path target) throws Exception {
        var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        var request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("User-Agent", "wikipedia-mobile-tests/1.0")
                .GET()
                .build();

        var resp = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() / 100 != 2) {
            String body = new String(resp.body(), StandardCharsets.UTF_8);
            if (body.length() > 500) body = body.substring(0, 500) + "...";
            log.error("HTTP {} while downloading APK from {}\nResponse snippet:\n{}",
                    resp.statusCode(), url, body);
            throw new IllegalStateException("HTTP " + resp.statusCode() + " while downloading APK");
        }

        Path tmp = target.resolveSibling(target.getFileName() + ".part");
        Files.write(tmp, resp.body());
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Создаёт локальный {@link AndroidDriver} с опциями UiAutomator2.
     *
     * <p>Порядок:
     * <ol>
     *   <li>Загрузка {@link LocalConfig};</li>
     *   <li>Разрешение пути к APK (локально/скачивание/кеш);</li>
     *   <li>Проверка доступности Appium по {@code /status};</li>
     *   <li>Сборка {@link UiAutomator2Options} и запуск сессии.</li>
     * </ol>
     *
     * @param capabilities входные капабилити (не используются напрямую)
     * @return активный {@link WebDriver}
     * @throws IllegalStateException при ошибке запуска драйвера
     */
    @Override
    public @NotNull WebDriver createDriver(@Nullable Capabilities capabilities) {
        LocalConfig cfg = ConfigFactory.create(LocalConfig.class, System.getProperties());

        String appPath = resolveAppPath(cfg);
        URL serverUrl = toUrl(cfg.getAppiumServerUrl(), "appium.server.url");

        ensureAppiumAlive(serverUrl);

        log.info("Starting local Android driver");
        log.info("Appium server: {}", serverUrl);
        log.info("Device: name='{}', platform='{} {}'",
                cfg.getDeviceName(), ANDROID, cfg.getPlatformVersion());
        log.info("App: path='{}', package='{}', activity='{}'",
                appPath, cfg.getAppPackage(), cfg.getAppActivity());

        UiAutomator2Options options = new UiAutomator2Options()
                .setAutomationName(ANDROID_UIAUTOMATOR2)
                .setPlatformName(ANDROID)
                .setPlatformVersion(cfg.getPlatformVersion())
                .setDeviceName(cfg.getDeviceName())
                .setApp(appPath)
                .setAppPackage(cfg.getAppPackage())
                .setAppActivity(cfg.getAppActivity())
                .setFullReset(true);

        try {
            return new AndroidDriver(serverUrl, options);
        } catch (RuntimeException e) {
            log.error("""
                            Failed to create AndroidDriver.
                            Check:
                              1) Appium server: {}
                              2) Device: deviceName='{}', platformVersion='{}' (adb devices / эмулятор запущен?)
                              3) APK path exists and is readable: {}
                              4) appPackage='{}', appActivity='{}'
                            Cause: {}
                            """,
                    serverUrl, cfg.getDeviceName(), cfg.getPlatformVersion(),
                    appPath, cfg.getAppPackage(), cfg.getAppActivity(),
                    e, e);
            throw new IllegalStateException("AndroidDriver creation failed. See logs.", e);
        }
    }

    /**
     * Разрешает путь к приложению из {@link LocalConfig}: напрямую, скачиванием по URL или через значения по умолчанию.
     *
     * @param cfg конфигурация запуска
     * @return абсолютный путь к APK
     * @throws IllegalStateException при невозможности найти или скачать APK
     */
    private String resolveAppPath(LocalConfig cfg) {
        String app = trimToNull(cfg.getApp());

        if (app != null) {
            if (isHttp(app)) {
                Path target = Path.of(cfg.getAppDir()).resolve(fileNameOrDefault(app, cfg));
                try {
                    Files.createDirectories(Path.of(cfg.getAppDir()));
                    if (Files.notExists(target)) {
                        log.info("Downloading APK from Owner 'app' URL: {}", app);
                        download(app, target);
                        log.info("APK saved: {} ({} bytes)", target.toAbsolutePath(), Files.size(target));
                    } else {
                        log.info("Using cached APK: {}", target.toAbsolutePath());
                    }
                    return target.toAbsolutePath().toString();
                } catch (Exception e) {
                    log.error("Failed to download APK from '{}'. Cause: {}", app, e, e);
                    throw new IllegalStateException("Download failed for 'app' URL: " + app, e);
                }
            } else {
                Path p = Path.of(app);
                if (Files.exists(p)) {
                    log.info("Using APK from Owner 'app' path: {}", p.toAbsolutePath());
                    return p.toAbsolutePath().toString();
                } else {
                    log.error("APK not found at Owner 'app' path: {}", p.toAbsolutePath());
                    throw new IllegalStateException("APK not found: " + p.toAbsolutePath());
                }
            }
        }

        Path dir = Path.of(cfg.getAppDir());
        Path target = dir.resolve(cfg.getAppFilename());
        String url = cfg.getAppUrl();

        try {
            Files.createDirectories(dir);
            if (Files.notExists(target)) {
                log.info("Owner 'app' not set. Downloading default APK: {}", url);
                download(url, target);
                log.info("APK saved: {} ({} bytes)", target.toAbsolutePath(), Files.size(target));
            } else {
                log.info("Using local default APK: {}", target.toAbsolutePath());
            }
            return target.toAbsolutePath().toString();
        } catch (Exception e) {
            log.error("Failed to prepare default APK at {}. Cause: {}", target.toAbsolutePath(), e, e);
            throw new IllegalStateException("Default APK prepare failed. See logs.", e);
        }
    }
}

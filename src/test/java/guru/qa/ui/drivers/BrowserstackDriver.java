package guru.qa.ui.drivers;

import com.codeborne.selenide.WebDriverProvider;
import guru.qa.ui.config.AuthConfig;
import guru.qa.ui.config.RemoteConfig;
import lombok.extern.slf4j.Slf4j;
import org.aeonbits.owner.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Провайдер удалённого драйвера для BrowserStack (App Automate).
 *
 * <p><b>Ключевые особенности:</b>
 * <ul>
 *   <li>Owner-конфиги: {@link AuthConfig} и {@link RemoteConfig} читаются с MERGE-приоритетом
 *       (System props → ENV → {@code classpath:${env}.properties} → fallback-файлы);</li>
 *   <li>W3C-капабилити для Appium 3: верхний уровень {@code platformName} + префиксы {@code appium:*}
 *       и {@code bstack:options};</li>
 *   <li>Резолвер приложения: поддержка {@code bs://}, HTTP(S)-URL или локального APK с автозагрузкой
 *       в BrowserStack через API;</li>
 *   <li>Failover для Free-плана: автоматическая замена девайса/ОС, если выбранный недоступен
 *       в бесплатном тарифе;</li>
 *   <li>Pass-through системных свойств: {@code -Dappium:*} в корневые капабилити, {@code -Dbstack.*}
 *       внутрь {@code bstack:options}.</li>
 * </ul>
 *
 * <p><b>Включение/отключение Free-plan fallback:</b> системное свойство
 * {@code -Dbs.freePlan.fallback=true|false} (по умолчанию: {@code true}).</p>
 */
@Slf4j
public class BrowserstackDriver implements WebDriverProvider {

    private static final String FREE_FALLBACK_DEVICE = "Google Pixel 3";
    private static final String FREE_FALLBACK_OS     = "9.0";

    /**
     * Создаёт удалённый {@link WebDriver} на BrowserStack с учётом W3C-капабилити,
     * Owner-конфигов и fallback-логики.
     *
     * @param incoming входные капабилити (могут быть {@code null}); будут расширены/переопределены
     * @return инициализированный {@link RemoteWebDriver}
     * @throws IllegalStateException при ошибках конфигурации, загрузки APK или старта сессии
     */
    @Override
    public @NotNull WebDriver createDriver(@Nullable Capabilities incoming) {
        AuthConfig auth     = ConfigFactory.create(AuthConfig.class, System.getProperties());
        RemoteConfig mobile = ConfigFactory.create(RemoteConfig.class, System.getProperties());

        URL hubUrl = toUrl(auth.getRemoteUrl(), "remoteUrl");

        MutableCapabilities caps = incoming == null ? new MutableCapabilities() : new MutableCapabilities(incoming);
        putIfAbsent(caps, "platformName", "Android");
        putIfAbsent(caps, "appium:automationName", "UiAutomator2");

        String appProperty = Objects.toString(mobile.getApp(), "").trim();
        if (appProperty.isEmpty()) {
            throw new IllegalStateException("remote.properties: 'app' пустой. Укажи bs://<app-id> или путь к APK внутри репо.");
        }
        String appForBs = resolveAppForBrowserStack(appProperty, auth);
        caps.setCapability("appium:app", appForBs);

        Map<String, Object> bstack = new HashMap<>();
        req(bstack, "deviceName", mobile.getDevice(), "remote.properties: 'device' пустой");
        req(bstack, "osVersion",  mobile.getOsVersion(), "remote.properties: 'os_version' пустой");

        putIfNotBlank(bstack, "projectName", mobile.getProject());
        putIfNotBlank(bstack, "buildName",   mobile.getBuild());
        putIfNotBlank(bstack, "sessionName", mobile.getSessionName());
        putIfNotBlank(bstack, "appiumVersion", nullTo(mobile.getAppiumVersion(), "2.19.0"));
        putIfNotBlank(bstack, "debug",       nullTo(mobile.getDebug(),       "true"));
        putIfNotBlank(bstack, "video",       nullTo(mobile.getVideo(),       "true"));

        propagateSystemPropsToBstackOptions(bstack, "bstack.");
        propagatePrefixedSystemProps(caps, "appium:");

        putIfNotBlank(bstack, "userName",  auth.getUserName());
        putIfNotBlank(bstack, "accessKey", auth.getKey());

        caps.setCapability("bstack:options", bstack);

        log.info("=== BrowserStack session ===");
        log.info("Hub: {}", hubUrl);
        log.info("Project/Build/Name: {}/{}/{}", mobile.getProject(), mobile.getBuild(), mobile.getSessionName());
        log.info("Device: {} / OS {}", bstack.get("deviceName"), bstack.get("osVersion"));
        log.info("App: {}", appForBs);
        log.info("User: {} / Key: {}", mask(auth.getUserName()), mask(auth.getKey()));

        boolean freeFallbackEnabled = !"false".equalsIgnoreCase(System.getProperty("bs.freePlan.fallback", "true"));

        try {
            return new RemoteWebDriver(hubUrl, caps);
        } catch (RuntimeException first) {
            if (freeFallbackEnabled && isFreePlanDeviceError(first)) {
                log.warn("BrowserStack Free plan limitation detected — retrying on {} / {}", FREE_FALLBACK_DEVICE, FREE_FALLBACK_OS);
                @SuppressWarnings("unchecked")
                Map<String, Object> bs = (Map<String, Object>) caps.getCapability("bstack:options");
                if (bs != null) {
                    bs.put("deviceName", FREE_FALLBACK_DEVICE);
                    bs.put("osVersion",  FREE_FALLBACK_OS);
                    String sName = Objects.toString(bs.get("sessionName"), "UI");
                    bs.put("sessionName", sName + " (free fallback)");
                    caps.setCapability("bstack:options", bs);
                }
                try {
                    return new RemoteWebDriver(hubUrl, caps);
                } catch (RuntimeException second) {
                    throw wrap(second, hubUrl, mobile, auth, caps);
                }
            }
            throw wrap(first, hubUrl, mobile, auth, caps);
        }
    }

    /* ===================== app resolver / uploader ===================== */

    /**
     * Преобразует значение {@code app} к формату, принимаемому BrowserStack:
     * <ul>
     *   <li>{@code bs://...} — возвращается как есть;</li>
     *   <li>HTTP(S) — отдаётся как URL (BrowserStack скачает самостоятельно);</li>
     *   <li>Локальный путь — загружается через API, возвращается {@code bs://<id>}.</li>
     * </ul>
     *
     * @param appProp исходное значение свойства {@code app}
     * @param auth    учётные данные для BrowserStack API
     * @return подготовленный идентификатор/URL приложения
     * @throws IllegalStateException если файл не найден или загрузка неуспешна
     */
    private static String resolveAppForBrowserStack(String appProp, AuthConfig auth) {
        if (appProp.startsWith("bs://")) return appProp;
        if (appProp.startsWith("http://") || appProp.startsWith("https://")) return appProp;

        Path p = Path.of(appProp);
        if (!Files.exists(p)) {
            p = Path.of(System.getProperty("user.dir"), appProp).normalize();
        }
        if (!Files.exists(p)) {
            throw new IllegalStateException("APK not found: " + appProp + " (cwd=" + System.getProperty("user.dir") + ")");
        }

        String uploaded = uploadApkToBrowserStack(p, auth);
        if (uploaded == null || !uploaded.startsWith("bs://")) {
            throw new IllegalStateException("BrowserStack upload failed or returned unexpected app_url: " + uploaded);
        }
        return uploaded;
    }

    /**
     * Загружает APK в BrowserStack App Automate и возвращает {@code bs://<app-id>}.
     *
     * @param apk  путь к APK
     * @param auth учётные данные для API
     * @return {@code bs://<app-id>} при успехе
     * @throws IllegalStateException при сетевых/IO ошибках или неожидаемом ответе API
     */
    private static String uploadApkToBrowserStack(Path apk, AuthConfig auth) {
        log.info("Uploading APK to BrowserStack: {}", apk);
        try {
            String boundary   = "----BSFormBoundary" + UUID.randomUUID();
            byte[] fileBytes  = Files.readAllBytes(apk);
            String filename   = apk.getFileName().toString();

            String partHeader = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n";
            String partFooter = "\r\n--" + boundary + "--\r\n";

            byte[] headerBytes = partHeader.getBytes();
            byte[] footerBytes = partFooter.getBytes();
            byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
            System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
            System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

            String basic = Base64.getEncoder().encodeToString((auth.getUserName() + ":" + auth.getKey()).getBytes());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-cloud.browserstack.com/app-automate/upload"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String json = resp.body();
                Matcher m = Pattern.compile("\"app_url\"\\s*:\\s*\"(bs://[^\"]+)\"").matcher(json);
                if (m.find()) {
                    String url = m.group(1);
                    log.info("Uploaded to BrowserStack: {}", url);
                    return url;
                }
                log.error("Upload ok but 'app_url' not found. Response: {}", json);
                return null;
            } else {
                log.error("Upload failed. HTTP {} Body: {}", resp.statusCode(), resp.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to upload APK to BrowserStack: " + e, e);
        }
    }

    /* ============================ utils ============================ */

    /**
     * Эвристика распознавания ошибки недоступности устройства в Free-плане.
     *
     * @param t исключение старта сессии
     * @return {@code true}, если сообщение указывает на ограничение бесплатного тарифа
     */
    private static boolean isFreePlanDeviceError(Throwable t) {
        String full = collectMsg(t).toLowerCase(Locale.ROOT);
        return full.contains("not available in free plan")
                || full.contains("available in free plan");
    }

    /**
     * Собирает сообщения из цепочки исключений.
     *
     * @param t корневое исключение
     * @return конкатенированная строка сообщений
     */
    private static String collectMsg(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            if (t.getMessage() != null) sb.append(t.getMessage()).append("\n");
            t = t.getCause();
        }
        return sb.toString();
    }

    /**
     * Оборачивает первичную ошибку и пишет расширенную диагностику капабилити/конфигов.
     */
    private static IllegalStateException wrap(RuntimeException e, URL hubUrl, RemoteConfig mobile,
                                              AuthConfig auth, Capabilities caps) {
        log.error("""
                  Failed BS session.
                  Hub={} device={} os={} app={} user={}
                  Caps={}
                  Cause={}
                  """,
                hubUrl, mobile.getDevice(), mobile.getOsVersion(), mobile.getApp(), auth.getUserName(),
                caps, e.toString(), e);
        return new IllegalStateException("BrowserStack driver creation failed. See logs.", e);
    }

    /** Возвращает {@code def}, если строка {@code v} пуста или {@code null}. */
    private static String nullTo(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    /** Устанавливает capability, если ключ отсутствует. */
    private static void putIfAbsent(MutableCapabilities caps, String key, Object value) {
        if (caps.getCapability(key) == null && value != null) caps.setCapability(key, value);
    }

    /** Кладёт пару в карту, если значение непустое. */
    private static void putIfNotBlank(Map<String, Object> map, String key, String val) {
        if (val != null && !val.isBlank()) map.put(key, val);
    }

    /** Проверяет, что обязательный параметр непустой, и записывает его в карту. */
    private static void req(Map<String, Object> map, String key, String val, String err) {
        if (val == null || val.isBlank()) throw new IllegalStateException(err);
        map.put(key, val);
    }

    /** Превращает строку в {@link URL} с внятной ошибкой для ключа. */
    private static URL toUrl(String raw, String key) {
        try {
            return URI.create(raw).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Bad " + key + ": " + raw, e);
        }
    }

    /** Маскирует чувствительные значения для логов. */
    private static String mask(String s) {
        if (s == null || s.isBlank()) return "<empty>";
        return s.length() <= 4 ? "***" : s.substring(0, 2) + "***";
    }

    /**
     * Переносит системные свойства с указанным префиксом в capabilities
     * (используется для {@code -Dappium:*}).
     */
    private static void propagatePrefixedSystemProps(MutableCapabilities caps, String prefix) {
        Properties sys = System.getProperties();
        for (Map.Entry<Object, Object> e : sys.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (key.startsWith(prefix)) {
                caps.setCapability(key, e.getValue());
            }
        }
    }

    /**
     * Переносит системные свойства {@code -Dbstack.*} внутрь {@code bstack:options}
     * с обрезанием префикса ({@code bstack.} → ключ в опциях).
     */
    private static void propagateSystemPropsToBstackOptions(Map<String, Object> bstack, String prefix) {
        Properties sys = System.getProperties();
        for (Map.Entry<Object, Object> e : sys.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (key.startsWith(prefix)) {
                String shortKey = key.substring(prefix.length());
                bstack.put(shortKey, e.getValue());
            }
        }
    }
}

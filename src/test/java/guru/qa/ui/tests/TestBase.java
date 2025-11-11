package guru.qa.ui.tests;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import guru.qa.ui.app.App;
import guru.qa.ui.drivers.BrowserstackDriver;
import guru.qa.ui.drivers.LocalDriver;
import guru.qa.ui.helpers.Attach;
import guru.qa.ui.logging.PrettySelenideRuListener;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.MDC;

import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.hasWebDriverStarted;

/**
 * Базовый класс мобильных UI-тестов / <b>Mobile UI Test Base</b>.
 *
 * <p><b>Назначение:</b> единая инициализация и завершение сессии Appium/Selenide для каждого теста:
 * выбор драйвера по {@code deviceHost=local|remote}, настройка Selenide и лог-слушателей,
 * создание сессии перед тестом и сбор аттачей после теста.</p>
 *
 * <p><b>Правила:</b>
 * <ul>
 *   <li>Сессия создаётся <i>на каждый тест</i> ( {@code open()} в {@link #beforeEach(TestInfo)} ).</li>
 *   <li>Шаги — только через {@code Allure.step(...)} в тестах (без {@code @Step}).</li>
 *   <li>Завершение: для <i>remote</i> — pageSource → close → video; для <i>local</i> — screenshot → pageSource → close.</li>
 * </ul>
 * </p>
 *
 * <p><b>EN:</b> Provides per-test session lifecycle: picks driver by {@code deviceHost},
 * configures Selenide & listeners, opens session before each test and attaches artifacts after.</p>
 */
public class TestBase {

    /**
     * Хелпер для чтения {@code deviceHost} из System props/ENV.
     * <br><b>EN:</b> Helper to read {@code deviceHost} from system properties or environment.
     */
    @SuppressWarnings("unused")
    private static String deviceHost() {
        String fromSys = System.getProperty("deviceHost");
        if (fromSys != null && !fromSys.isBlank()) return fromSys.trim().toLowerCase();
        String fromEnv = System.getenv("DEVICE_HOST");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim().toLowerCase();
        return "local";
    }

    /**
     * Глобальная конфигурация Selenide и лог-слушателей.
     * <br><b>EN:</b> Global Selenide configuration and log listeners.
     */
    @BeforeAll
    static void beforeAll() {
        String deviceHost = System.getProperty("deviceHost", "local");
        Configuration.browser = "remote".equalsIgnoreCase(deviceHost)
                ? BrowserstackDriver.class.getName()
                : LocalDriver.class.getName();

        Configuration.browserSize = null;
        Configuration.timeout = 30_000;
        Configuration.pageLoadTimeout = 0L;
        Configuration.pageLoadStrategy = "none";
        Configuration.reportsFolder = "allure-results";

        if (!SelenideLogger.hasListener("pretty-ru")) {
            SelenideLogger.addListener("pretty-ru", new PrettySelenideRuListener());
        }
        if (!SelenideLogger.hasListener("AllureSelenide")) {
            SelenideLogger.addListener("AllureSelenide",
                    new AllureSelenide()
                            .savePageSource(true)
                            .screenshots(true)
                            .includeSelenideSteps(false)
            );
        }
    }

    /**
     * Перед тестом: сохраняем имя теста в MDC и открываем сессию.
     * <br><b>EN:</b> Put test name into MDC and open session.
     */
    @BeforeEach
    void beforeEach(TestInfo info) {
        if (info != null && info.getDisplayName() != null) {
            MDC.put("test", info.getDisplayName());
        }
        open();
    }

    /**
     * После теста: собираем артефакты и закрываем сессию.
     * <br><b>EN:</b> Collect artifacts and close session.
     */
    @AfterEach
    void afterEach() {
        if (!hasWebDriverStarted()) {
            MDC.remove("test");
            return;
        }

        String deviceHost = System.getProperty("deviceHost", "local");
        try {
            if ("remote".equalsIgnoreCase(deviceHost)) {
                String sessionId = Selenide.sessionId() != null ? Selenide.sessionId().toString() : null;
                try { Attach.pageSource(); } catch (Throwable ignored) {}
                closeWebDriver();
                App.reset();
                if (sessionId != null) {
                    try { Attach.addVideo(sessionId); } catch (Throwable ignored) {}
                }
            } else {
                try { Attach.screenshotAs("Last screenshot"); } catch (Throwable ignored) {}
                try { Attach.pageSource(); } catch (Throwable ignored) {}
                closeWebDriver();
                App.reset();
            }
        } finally {
            MDC.remove("test");
        }
    }
}

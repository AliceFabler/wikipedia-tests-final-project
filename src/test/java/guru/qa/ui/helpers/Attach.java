package guru.qa.ui.helpers;

import io.qameta.allure.Attachment;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.nio.charset.StandardCharsets;

import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

/**
 * Хелпер для вложений в Allure-отчёт.
 *
 * <p>Все методы статические и готовы к вызову из хуков/тестов. Аттачи
 * маркируются типами, понятными Allure, и корректно отображаются в отчёте.</p>
 *
 * <p><b>Примеры:</b>
 * <pre>{@code
 * Attach.screenshotAs("После логина");
 * Attach.pageSource();
 * Attach.addVideo(sessionId);
 * }</pre>
 */
@SuppressWarnings("UnusedReturnValue")
public class Attach {

    /**
     * Добавляет PNG-скриншот активного экрана в Allure.
     *
     * @param attachName имя вложения в отчёте
     * @return байты PNG-изображения
     */
    @Attachment(value = "{attachName}", type = "image/png")
    public static byte[] screenshotAs(String attachName) {
        return ((TakesScreenshot) getWebDriver()).getScreenshotAs(OutputType.BYTES);
    }

    /**
     * Добавляет исходный код текущей страницы/экрана в виде текста UTF-8.
     *
     * @return байты текстового источника страницы
     */
    @Attachment(value = "Page source", type = "text/plain")
    public static byte[] pageSource() {
        return getWebDriver().getPageSource().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Встраивает HTML-плейер с видеозаписью BrowserStack в Allure.
     *
     * @param sessionId идентификатор сессии в BrowserStack
     * @return HTML-разметка с тегом &lt;video&gt; и источником на mp4
     */
    @Attachment(value = "Video", type = "text/html", fileExtension = ".html")
    public static String addVideo(String sessionId) {
        return "<html><body><video width='100%' height='100%' controls autoplay><source src='"
                + Browserstack.videoUrl(sessionId)
                + "' type='video/mp4'></video></body></html>";
    }
}

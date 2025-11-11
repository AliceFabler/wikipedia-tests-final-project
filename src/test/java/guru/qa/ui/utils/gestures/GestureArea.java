package guru.qa.ui.utils.gestures;

import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

/**
 * Прямоугольная область (left/top/width/height) для жестов по region.
 */
public record GestureArea(int left, int top, int width, int height) {

    /** Область из Selenium-элемента. */
    public static GestureArea fromElement(WebElement element) {
        Rectangle r = element.getRect();
        return new GestureArea(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /** Область из SelenideAppiumElement (удобно для проекта). */
    public static GestureArea fromElement(SelenideAppiumElement element) {
        Rectangle r = element.getRect();
        return new GestureArea(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /** Весь вьюпорт указанного окна. */
    public static GestureArea fromViewport(WebDriver driver) {
        Dimension d = driver.manage().window().getSize();
        return new GestureArea(0, 0, d.getWidth(), d.getHeight());
    }

    /** Весь вьюпорт текущего окна (WebDriverRunner). */
    public static GestureArea fromViewport() {
        WebDriver d = WebDriverRunner.getWebDriver();
        Dimension s = d.manage().window().getSize();
        return new GestureArea(0, 0, s.getWidth(), s.getHeight());
    }

    /** Центр X области. */
    public int centerX() {
        return left + width / 2;
    }

    /** Центр Y области. */
    public int centerY() {
        return top + height / 2;
    }

    /**
     * Вернуть "внутреннюю" область, сдвинув границы внутрь на dx/dy (неотрицательные размеры).
     * Пример: area.inset(16, 24) — сузить область по всем сторонам.
     */
    public GestureArea inset(int dx, int dy) {
        int nl = left + dx;
        int nt = top + dy;
        int nw = Math.max(0, width - 2 * dx);
        int nh = Math.max(0, height - 2 * dy);
        return new GestureArea(nl, nt, nw, nh);
    }

    /**
     * Удобные аргументы для executeScript("mobile:*", args):
     * {"left":..., "top":..., "width":..., "height":...}
     */
    public Map<String, Object> toArgs() {
        return Map.of(
                "left", left,
                "top", top,
                "width", width,
                "height", height
        );
    }
}

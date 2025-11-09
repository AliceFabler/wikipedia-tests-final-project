package guru.qa.ui.utils.gestures;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Прямоугольная область (left/top/width/height), когда жест исполняется не по элементу, а по "region".
 */
public record GestureArea(int left, int top, int width, int height) {

    /**
     * Создать область из Selenium-элемента.
     */
    public static GestureArea fromElement(WebElement element) {
        Rectangle r = element.getRect();
        return new GestureArea(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * Весь вьюпорт текущего окна.
     */
    public static GestureArea fromViewport(WebDriver driver) {
        Dimension d = driver.manage().window().getSize();
        return new GestureArea(0, 0, d.getWidth(), d.getHeight());
    }
}

package guru.qa.ui.screens.components;

import com.codeborne.selenide.appium.SelenideAppiumElement;
import guru.qa.ui.app.App;
import lombok.extern.slf4j.Slf4j;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;
import static io.appium.java_client.AppiumBy.id;

/**
 * Нижняя панель вкладок (Bottom Tab Bar) приложения Wikipedia (Android, alpha).
 *
 * <p>Идентификаторы вкладок:</p>
 * <ul>
 *   <li>{@code org.wikipedia.alpha:id/nav_tab_explore} — Лента / Explore</li>
 *   <li>{@code org.wikipedia.alpha:id/nav_tab_reading_lists} — Сохранённые / Saved</li>
 *   <li>{@code org.wikipedia.alpha:id/nav_tab_search} — Найти / Search</li>
 *   <li>{@code org.wikipedia.alpha:id/nav_tab_edits} — Активность / Edits</li>
 *   <li>{@code org.wikipedia.alpha:id/nav_tab_more} — Ещё / More</li>
 * </ul>
 *
 * <p><b>EN:</b> Bottom tab bar component with stable resource-ids. Each navigation asserts
 * {@code selected=true} on the target tab.</p>
 */
@Slf4j
public class BottomTabBar {

    /** Вкладка «Лента / Explore». */
    public final SelenideAppiumElement tabExplore = $(id("org.wikipedia.alpha:id/nav_tab_explore"));
    /** Вкладка «Сохранённые / Saved». */
    public final SelenideAppiumElement tabSaved = $(id("org.wikipedia.alpha:id/nav_tab_reading_lists"));

    /**
     * Открыть вкладку «Сохранённые / Saved» и дождаться её активации.
     *
     * <p><b>EN:</b> Open “Saved” tab and verify it's selected.</p>
     */
    public void openSaved() {
        step("Открыть вкладку «Сохранённые / Saved»", () -> {
            switchTo("Сохранённые / Saved", tabSaved);
            App.screens().saved.shouldBeOpen();
        });
    }

    /**
     * Общая логика переключения на вкладку с вложенными шагами.
     *
     * <p><b>EN:</b> Internal switch routine with nested Allure steps and selection check.</p>
     *
     * @param humanName метка вкладки для логов/отчёта (RU/EN)
     * @param tab       element handle for the tab
     */
    private void switchTo(final String humanName, final SelenideAppiumElement tab) {
        step("Открыть вкладку: " + humanName, () -> {
            log.info("Навигация к табу: {}", humanName);

            step("Таб доступен для клика (enabled)", () ->
                    tab.shouldBe(enabled)
            );

            step("Клик по табу", () -> tab.click());

            step("Проверка: таб выбран (selected=true)", () -> {
                tab.shouldHave(attribute("selected", "true"));
                log.info("Таб '{}' активен (selected=true)", humanName);
            });
        });
    }
}

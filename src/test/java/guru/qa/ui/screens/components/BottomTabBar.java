package guru.qa.ui.screens.components;

import com.codeborne.selenide.appium.SelenideAppiumElement;
import guru.qa.ui.app.App;
import lombok.extern.slf4j.Slf4j;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;
import static io.appium.java_client.AppiumBy.id;

/*
 * 🎯 MASTER PROMPT (v1, from scratch)
 * Создай компонент нижней панели вкладок Wikipedia (Android) на базе Selenide-Appium:
 *   — используем SelenideAppiumElement и SelenideAppium.$(...);
 *   — публичные методы: openExplore(), openSaved(), openSearch(), openEdits(), openMore();
 *   — в методах делать Allure-шага и проверять, что таб активен (selected=true).
 *
 * 🔄 ENHANCEMENTS (v2)
 *   — вложенные Allure-шага внутри switchTo(...), читаемые логи;
 *   — никаких throws/исключений в сигнатурах, понятные действия.
 *
 * 🚀 ENHANCEMENTS (v3)
 *   — двуязычные (RU/EN) Javadoc/шаги; табы по стабильным resource-id из page source.
 */

/**
 * Компонент нижней панели вкладок / <b>Bottom Tab Bar</b> (Wikipedia Android, alpha).
 * <p>Идентификаторы вкладок (из page source):</p>
 * <ul>
 *   <li><code>org.wikipedia.alpha:id/nav_tab_explore</code> — Лента / Explore</li>
 *   <li><code>org.wikipedia.alpha:id/nav_tab_reading_lists</code> — Сохранённые / Saved</li>
 *   <li><code>org.wikipedia.alpha:id/nav_tab_search</code> — Найти / Search</li>
 *   <li><code>org.wikipedia.alpha:id/nav_tab_edits</code> — Активность / Edits</li>
 *   <li><code>org.wikipedia.alpha:id/nav_tab_more</code> — Ещё / More</li>
 * </ul>
 * <p>Каждый переход выполняет клик и верификацию <code>selected=true</code> у целевой вкладки.</p>
 */
@Slf4j
public class BottomTabBar {

    // --- Локаторы вкладок (Selenide-Appium) ---
    public final SelenideAppiumElement tabExplore = $(id("org.wikipedia.alpha:id/nav_tab_explore"));
    public final SelenideAppiumElement tabSaved = $(id("org.wikipedia.alpha:id/nav_tab_reading_lists"));

    /**
     * Открыть «Сохранённые / Saved».
     */
    public void openSaved() {
        step("Открыть вкладку «Сохранённые / Saved»", () -> {
            switchTo("Сохранённые / Saved", tabSaved);
            App.screens().saved.shouldBeOpen();
        });
    }

    /**
     * Общая логика переключения на таб с вложенными Allure-шагами.
     *
     * @param humanName человекочитаемое имя (RU/EN) для логов/отчёта
     * @param tab       элемент вкладки (SelenideAppiumElement)
     */
    private void switchTo(final String humanName, final SelenideAppiumElement tab) {
        step("Открыть вкладку: " + humanName, () -> {
            log.info("Навигация к табу: {}", humanName);

            step("Таб доступен для клика (enabled)", () ->
                    tab.shouldBe(enabled)
            );

            // ВАЖНО: лямбда, а не method reference — иначе 'click' ambiguous для Allure.step
            step("Клик по табу", () -> tab.click());

            step("Проверка: таб выбран (selected=true)", () -> {
                tab.shouldHave(attribute("selected", "true"));
                log.info("Таб '{}' активен (selected=true)", humanName);
            });
        });
    }
}

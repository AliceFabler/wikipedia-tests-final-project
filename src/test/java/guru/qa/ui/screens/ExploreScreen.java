package guru.qa.ui.screens;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import guru.qa.ui.app.App;
import guru.qa.ui.utils.gestures.ScrollIntoView;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.Arrays;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;
import static io.appium.java_client.AppiumBy.id;

/* ============================================================================
   🎯 MASTER PROMPT (RU) — ExploreScreen (финальная версия)

   Назначение:
   • Page Object экрана «Explore» (Wikipedia Android, alpha): поиск, объявление,
     карточка «Wikipedia games», блок «Top read», прокрутка до нужной карточки,
     работа с лентой по датам (day header) и сбор верхнеуровневых карточек.

   Инварианты (канон проекта):
   • Appium 3 + UIAutomator2 (W3C), XPath 2.0; ТОЛЬКО стабильные локаторы (id / accessibilityId / XPath2).
   • Элементы — SelenideAppiumElement/Collection; без UiSelector/TouchAction/Thread.sleep.
   • Ожидания через should* и ScrollIntoView (внутри — Selenide.Wait()).
   • Правило клика: нажимаем только когда visible=true, enabled=true, attribute(clickable)="true" (см. clickWhenReady).

   Публичные шаги (использовать в тестах):
   • shouldBeVisible()
   • openSearch(), tapVoiceSearch(), dismissAnnouncementIfShown()
   • openWikipediaGames()
   • shouldSeeTopRead(), openTopReadItem(index)
   • scrollToCard(String... titles)                        — доскроллить до карточки по заголовку (RU/EN)
   • shouldSeeSectionHeader(String... titles)              — проверить видимость заголовка карточки
   • scrollToDate(String dateText)                         — доскроллить до секции нужной даты (day header)
   • collectCardRootsForDate(String dateText)              — собрать корневые карточки за эту дату
   • getCardRootForSectionOnDate(dateText, sectionTitles)  — корневая карточка «Section title» на конкретной дате
   • openFeaturedArticleFirstItem()                        — открыть первый кликабельный элемент в «Featured article»
   ============================================================================ */

@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class ExploreScreen {

    // ───────── Служебные константы заголовков ─────────
    private static final String CARD_HEADER_ID = "org.wikipedia.alpha:id/view_card_header_title";
    // ───────── Специфичный контент «Featured article» ─────────
    private static final String FEATURED_CONTENT_ID =
            "org.wikipedia.alpha:id/view_featured_article_card_content_container";
    // ───────── Верхняя поисковая панель ─────────
    private final SelenideAppiumElement searchContainer =
            $(id("org.wikipedia.alpha:id/search_container"));
    // ───────── Объявление «Customize your Explore feed» ─────────
    private final SelenideAppiumElement announcementCard =
            $(id("org.wikipedia.alpha:id/view_announcement_container"));
    private final SelenideAppiumElement announcementOkBtn =
            $(id("org.wikipedia.alpha:id/view_announcement_action_negative"));
    // ───────── Контейнер ленты ─────────
    private final SelenideAppiumElement feedView =
            $(id("org.wikipedia.alpha:id/feed_view"));

    // ═════════════════════ Actions / Checks ═════════════════════

    /**
     * Единый «кликер»: visible + enabled + attribute(clickable)=true → tap()
     */
    private static void clickWhenReady(SelenideAppiumElement el, String name) {
        el.shouldBe(Condition.visible.because(name + " должен(а) быть видим(а)"))
                .shouldBe(Condition.enabled.because(name + " должен(а) быть доступен/доступна"))
                .shouldHave(Condition.attribute("clickable", "true")
                        .because(name + " должен(а) быть кликабельн(ым/ой)"))
                .tap();
    }

    public ExploreScreen shouldBeVisible() {
        return step("Экран Explore отображается", () -> {
            App.components().bottomTabBar.tabExplore.shouldBe(Condition.exist.because("Нижняя вкладка Explore должна существовать"));
            searchContainer.shouldBe(Condition.visible.because("Карточка поиска должна быть видима"));
            return this;
        });
    }

    public void openSearch() {
        step("Открыть поиск (тап по карточке поиска)", () ->
                clickWhenReady(searchContainer, "Карточка поиска"));
    }

    // ───────── Дополнительно: прокрутка до карточки по заголовку (RU/EN) ─────────

    public void dismissAnnouncementIfShown() {
        step("Закрыть объявление «Customize your Explore feed» (Got it), если показано", () -> {
            if (announcementCard.exists() && announcementCard.is(Condition.visible)) {
                clickWhenReady(announcementOkBtn, "Кнопка «Got it»");
            }
        });
    }

    private By headerBy(String... titles) {
        String alternation = String.join("|",
                Arrays.stream(titles)
                        .map(s -> s.toLowerCase().replace("'", "\\'"))
                        .toArray(String[]::new)
        );
        return By.xpath(
                "//android.widget.TextView[@resource-id='" + CARD_HEADER_ID + "' " +
                        "and matches(lower-case(@text), '^(" + alternation + ")$')]"
        );
    }

    private SelenideAppiumElement headerEl(String... titles) {
        return $(headerBy(titles));
    }

    public ExploreScreen scrollToCard(String... titles) {
        String joined = String.join(" / ", titles);
        return step("Прокрутить ленту до карточки «" + joined + "» и довести её в поле видимости", () -> {
            ScrollIntoView.intoView(feedView, headerEl(titles), Duration.ofSeconds(60));
            headerEl(titles).shouldBe(Condition.visible);
            return this;
        });
    }

    // ───────── Работа с датами (day header) и корневыми карточками ─────────

    public ExploreScreen shouldSeeSectionHeader(String... titles) {
        String joined = String.join(" / ", titles);
        return step("Заголовок карточки «" + joined + "» видим", () -> {
            headerEl(titles).shouldBe(Condition.visible);
            return this;
        });
    }

    private By cardRootBySectionTitle(String... titles) {
        // //*[@resource-id='...:id/feed_view']/android.widget.LinearLayout[descendant::*[@resource-id='...:id/view_card_header_title' and matches(lower-case(@text), '^(...)$')]]
        String alternation = String.join("|",
                Arrays.stream(titles)
                        .map(s -> s.toLowerCase().replace("'", "\\'"))
                        .toArray(String[]::new)
        );
        return By.xpath(
                "//*[@resource-id='org.wikipedia.alpha:id/feed_view']" +
                        "/android.widget.LinearLayout[" +
                        "descendant::*[@resource-id='" + CARD_HEADER_ID + "' " +
                        "and matches(lower-case(@text), '^(" + alternation + ")$')]]"
        );
    }

    // ───────── Внутренние помощники ─────────

    /**
     * Открыть первый кликабельный элемент внутри карточки 'Featured article'.
     */
    public void openFeaturedArticleFirstItem() {
        step("Открыть первый кликабельный элемент внутри карточки «Featured article»", () -> {
            scrollToCard("Featured article", "избранная статья"); // гарантируем видимость
            SelenideAppiumElement featuredRoot =
                    $(cardRootBySectionTitle("Featured article", "избранная статья"));

            SelenideAppiumElement firstClickable =
                    $(featuredRoot.$(id(FEATURED_CONTENT_ID)));

            // Правило кликабельности проекта
            firstClickable
                    .shouldBe(Condition.visible.because("Контент 'Featured article' должен быть видим"))
                    .shouldBe(Condition.enabled.because("Контент 'Featured article' должен быть доступен"))
                    .shouldHave(Condition.attribute("clickable", "true")
                            .because("Контент 'Featured article' должен быть кликабельным"))
                    .tap();
        });
    }
}

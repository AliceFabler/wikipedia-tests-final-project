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

/**
 * Экран «Explore» (Wikipedia Android).
 *
 * <p><b>Назначение:</b> Page Object ленты «Explore»: поиск, закрытие объявления,
 * прокрутка до карточек по заголовку (RU/EN), работа с датами и открытие контента
 * «Featured article».</p>
 *
 * <p><b>Инварианты проекта:</b> Appium 3 + UiAutomator2 (W3C), XPath 2.0;
 * только стабильные локаторы (id / accessibilityId / XPath2); элементы —
 * {@code SelenideAppiumElement}; без UiSelector/TouchAction; ожидания через
 * {@code should*} и утилиту {@link ScrollIntoView}.</p>
 *
 * <p><b>Правило клика:</b> нажимаем элемент только при
 * {@code visible=true}, {@code enabled=true}, {@code attribute(clickable)="true"}.</p>
 *
 * <p><b>Публичные шаги:</b>
 * <ul>
 *   <li>{@link #shouldBeVisible()}</li>
 *   <li>{@link #openSearch()}</li>
 *   <li>{@link #dismissAnnouncementIfShown()}</li>
 *   <li>{@link #scrollToCard(String...)}</li>
 *   <li>{@link #shouldSeeSectionHeader(String...)}</li>
 *   <li>{@link #openFeaturedArticleFirstItem()}</li>
 * </ul>
 * </p>
 *
 * <p><b>EN:</b> Explore screen Page Object with search, announcement dismissal,
 * scrolling to section cards by title (RU/EN), date-aware helpers, and opening the
 * first item of “Featured article”. Clicks obey the project’s clickability rule.</p>
 */
@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class ExploreScreen {

    private static final String CARD_HEADER_ID = "org.wikipedia.alpha:id/view_card_header_title";
    private static final String FEATURED_CONTENT_ID =
            "org.wikipedia.alpha:id/view_featured_article_card_content_container";

    private final SelenideAppiumElement searchContainer =
            $(id("org.wikipedia.alpha:id/search_container"));

    private final SelenideAppiumElement announcementCard =
            $(id("org.wikipedia.alpha:id/view_announcement_container"));
    private final SelenideAppiumElement announcementOkBtn =
            $(id("org.wikipedia.alpha:id/view_announcement_action_negative"));

    private final SelenideAppiumElement feedView =
            $(id("org.wikipedia.alpha:id/feed_view"));

    /**
     * Единый «кликер» по правилу кликабельности проекта.
     * <p><b>EN:</b> Unified click helper that enforces visibility/enabled/clickable.</p>
     *
     * @param el   элемент
     * @param name имя для сообщений ожиданий
     */
    private static void clickWhenReady(SelenideAppiumElement el, String name) {
        el.shouldBe(Condition.visible.because(name + " должен(а) быть видим(а)"))
                .shouldBe(Condition.enabled.because(name + " должен(а) быть доступен/доступна"))
                .shouldHave(Condition.attribute("clickable", "true")
                        .because(name + " должен(а) быть кликабельн(ым/ой)"))
                .tap();
    }

    /**
     * Экран «Explore» отображается: вкладка Explore существует, карточка поиска видима.
     * <p><b>EN:</b> Verifies Explore tab presence and visible search card.</p>
     */
    public ExploreScreen shouldBeVisible() {
        return step("Экран Explore отображается", () -> {
            App.components().bottomTabBar.tabExplore.shouldBe(Condition.exist.because("Нижняя вкладка Explore должна существовать"));
            searchContainer.shouldBe(Condition.visible.because("Карточка поиска должна быть видима"));
            return this;
        });
    }

    /**
     * Открыть поиск тапом по карточке поиска.
     * <p><b>EN:</b> Open search by tapping the search card.</p>
     */
    public void openSearch() {
        step("Открыть поиск (тап по карточке поиска)", () ->
                clickWhenReady(searchContainer, "Карточка поиска"));
    }

    /**
     * Закрыть объявление «Customize your Explore feed» (Got it), если показано.
     * <p><b>EN:</b> Dismiss the Explore announcement if visible.</p>
     */
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

    /**
     * Прокрутить ленту до карточки с любым из заголовков (RU/EN) и убедиться, что она видима.
     * <p><b>EN:</b> Scroll to a card by any of the given section titles and ensure visibility.</p>
     */
    public ExploreScreen scrollToCard(String... titles) {
        String joined = String.join(" / ", titles);
        return step("Прокрутить ленту до карточки «" + joined + "» и довести её в поле видимости", () -> {
            ScrollIntoView.intoView(feedView, headerEl(titles), Duration.ofSeconds(60));
            headerEl(titles).shouldBe(Condition.visible);
            return this;
        });
    }

    /**
     * Проверить, что заголовок нужной карточки видим.
     * <p><b>EN:</b> Assert that a section header is visible.</p>
     */
    public ExploreScreen shouldSeeSectionHeader(String... titles) {
        String joined = String.join(" / ", titles);
        return step("Заголовок карточки «" + joined + "» видим", () -> {
            headerEl(titles).shouldBe(Condition.visible);
            return this;
        });
    }

    /**
     * Открыть первый кликабельный элемент карточки «Featured article».
     * <p><b>EN:</b> Open the first clickable item inside the “Featured article” card.</p>
     */
    public void openFeaturedArticleFirstItem() {
        step("Открыть первый кликабельный элемент внутри карточки «Featured article»", () -> {
            scrollToCard("Featured article", "случайная статья");
            SelenideAppiumElement featuredRoot =
                    $(By.xpath(
                            "//*[@resource-id='org.wikipedia.alpha:id/feed_view']" +
                                    "/android.widget.LinearLayout[" +
                                    "descendant::*[@resource-id='" + CARD_HEADER_ID + "' " +
                                    "and matches(lower-case(@text), '^(featured article|случайная статья)$')]]"
                    ));

            SelenideAppiumElement firstClickable =
                    $(featuredRoot.$(id(FEATURED_CONTENT_ID)));

            firstClickable
                    .shouldBe(Condition.visible.because("Контент 'Featured article' должен быть видим"))
                    .shouldBe(Condition.enabled.because("Контент 'Featured article' должен быть доступен"))
                    .shouldHave(Condition.attribute("clickable", "true")
                            .because("Контент 'Featured article' должен быть кликабельным"))
                    .tap();
        });
    }
}

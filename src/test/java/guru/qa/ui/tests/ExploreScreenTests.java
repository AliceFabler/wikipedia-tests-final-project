package guru.qa.ui.tests;

import guru.qa.ui.app.App;
import io.qameta.allure.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import static guru.qa.ui.allure.Steps.step;
import static io.qameta.allure.Allure.parameter;

/**
 * Тесты ленты Explore (Wikipedia Android).
 *
 * <p><b>Цель:</b> верифицировать базовые сценарии взаимодействия с лентой:
 * появление секции «In the news» после автоскролла и переход в экран статьи
 * из карточки «Featured article».</p>
 *
 * <p><b>Инварианты:</b> подготовка среды выполняется в {@code TestBase}; все действия
 * логируются шагами Allure; используются стабильные локаторы внутри Page Object’ов.</p>
 *
 * <p><b>EN:</b> Explore feed smoke tests: verify visibility of “In the news”
 * after auto-scroll and open an article from the “Featured article” card.</p>
 */
@Slf4j
@Epic("Wikipedia (Android)")
@Feature("Explore / Лента")
@Owner("Alice Fabler")
@Severity(SeverityLevel.NORMAL)
@Tags({@Tag("android"), @Tag("local"), @Tag("remote"), @Tag("wikipedia")})
public class ExploreScreenTests extends TestBase {

    /**
     * Пре-условия для каждого теста:
     * <ul>
     *   <li>Пропустить онбординг (если показан);</li>
     *   <li>Убедиться, что лента Explore отображается;</li>
     *   <li>Скрыть объявление (если присутствует).</li>
     * </ul>
     *
     * <p><b>EN:</b> Pre-conditions: skip onboarding (if shown), assert Explore is visible,
     * dismiss announcement if present.</p>
     */
    @BeforeEach
    @DisplayName("Пре-условия: пропуск онбординга и проверка ленты Explore")
    void beforeEachExplore() {
        step("Пропустить онбординг (если показан)", App.screens().onboarding::skipIfVisible);
        step("Убедиться, что открыта лента Explore; скрыть объявление, если показано", () -> {
            App.screens().explore.shouldBeVisible();
            App.screens().explore.dismissAnnouncementIfShown();
        });
    }

    /**
     * Сценарий: автоскролл до секции «In the news» / «В новостях» и проверка видимости её заголовка.
     *
     * <p><b>EN:</b> Auto-scroll to the “In the news” card and verify its header is visible.</p>
     */
    @Test
    @DisplayName("Лента: секция «In the news» отображается после автоскролла")
    @Story("Отображение секций ленты")
    @Description("Автоскролл до 'In the news' / 'В новостях' и проверка видимости её заголовка.")
    @Issue("HOMEWORK-1526")
    @AllureId("40938")
    void inTheNewsSectionPresent_onExplore() {
        final String sectionEn = "In the news";
        final String sectionRu = "В новостях";
        parameter("sectionEn", sectionEn);
        parameter("sectionRu", sectionRu);

        step("Доскроллить до карточки '" + sectionEn + "'", () -> App.screens().explore.scrollToCard(sectionEn, sectionRu));
        step("Заголовок '" + sectionEn + "' видим", () -> App.screens().explore.shouldSeeSectionHeader(sectionEn, sectionRu));
    }

    /**
     * Сценарий: прокрутка до «Featured article», тап по первому кликабельному элементу,
     * закрытие оверлеев и верификация открытия экрана статьи.
     *
     * <p><b>EN:</b> Scroll to “Featured article”, tap the first clickable item,
     * close overlays and assert the Article screen is open.</p>
     */
    @Test
    @DisplayName("Лента: открыть статью из карточки «Featured article»")
    @Story("Навигация из карточек ленты в экран статьи")
    @Description("Скролл до 'Featured article' → тап по первому кликабельному элементу → закрыть оверлеи → проверить экран статьи.")
    @Issue("HOMEWORK-1527")
    @AllureId("40939")
    void openArticleFromFeaturedArticle() {
        final String sectionEn = "Featured article";
        final String sectionRu = "Случайная статья";
        parameter("sectionEn", sectionEn);
        parameter("sectionRu", sectionRu);

        step("Доскроллить до карточки '" + sectionEn + "'", () -> App.screens().explore.scrollToCard(sectionEn, sectionRu));
        step("Нажать первый кликабельный элемент внутри карточки '" + sectionEn + "'", App.screens().explore::openFeaturedArticleFirstItem);
        step("Закрыть возможные оверлеи и проверить, что открыт экран статьи", () -> {
            App.components().overlays.closeAllIfShown();
            App.screens().article.shouldBeOpen();
        });
    }
}

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

@Slf4j
@Epic("Wikipedia (Android)")
@Feature("Explore / Лента")
@Owner("Alice Fabler")
@Severity(SeverityLevel.NORMAL)
@Tags({@Tag("android"), @Tag("local"), @Tag("remote"), @Tag("wikipedia")})
public class ExploreScreenTests extends TestBase {

    @BeforeEach
    @DisplayName("Пре-условия: пропуск онбординга и проверка ленты Explore")
    void beforeEachExplore() {
        step("Пропустить онбординг (если показан)", App.screens().onboarding::skipIfVisible);
        step("Убедиться, что открыта лента Explore; скрыть объявление, если показано", () -> {
            App.screens().explore.shouldBeVisible();
            App.screens().explore.dismissAnnouncementIfShown();
        });
    }

    @Test
    @DisplayName("Лента: секция «In the news» отображается после автоскролла")
    @Story("Отображение секций ленты")
    @Description("Автоскролл до карточки 'In the news' / 'В новостях' и проверка видимости её заголовка.")
    @Issue("HOMEWORK-1526")
    @AllureId("40938")
    void inTheNewsSectionPresent_onExplore() {
        final String sectionEn = "In the news";
        final String sectionRu = "в новостях";
        parameter("sectionEn", sectionEn);
        parameter("sectionRu", sectionRu);

        step("Доскроллить до карточки '" + sectionEn + "'", () -> App.screens().explore.scrollToCard(sectionEn, sectionRu));
        step("Заголовок '" + sectionEn + "' видим", () -> App.screens().explore.shouldSeeSectionHeader(sectionEn, sectionRu));
    }

    @Test
    @DisplayName("Лента: открыть статью из карточки «Featured article»")
    @Story("Навигация из карточек ленты в экран статьи")
    @Description("Скролл до 'Featured article' → тап по первому кликабельному элементу → закрыть оверлеи → проверить экран статьи.")
    @Issue("HOMEWORK-1527")
    @AllureId("40939")
    void openArticleFromFeaturedArticle() {
        final String sectionEn = "Featured article";
        final String sectionRu = "избранная статья";
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

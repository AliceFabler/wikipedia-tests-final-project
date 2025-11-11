package guru.qa.ui.tests;

import guru.qa.ui.app.App;
import guru.qa.ui.utils.DataExtractor;
import guru.qa.ui.utils.Variables;
import io.qameta.allure.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import static guru.qa.ui.allure.Steps.step;
import static io.qameta.allure.Allure.parameter;

/**
 * UI-тест: добавление статьи в «Сохранённое» из поиска (Wikipedia Android).
 *
 * <p><b>Цель:</b> найти статью, открыть её, сохранить, дождаться подтверждения через Snackbar
 * и убедиться, что статья появилась в дефолтном списке «Сохранённое».</p>
 *
 * <p><b>Предусловия:</b> драйвер и окружение инициализируются в {@code TestBase};
 * сеть доступна; приложение установлено. Онбординг может быть пропущен шагом теста.</p>
 *
 * <p><b>Основные шаги:</b>
 * <ol>
 *   <li>Пропустить онбординг (если показан) и открыть поиск с экрана Explore.</li>
 *   <li>Ввести запрос, открыть первый результат, сохранить статью и дождаться Snackbar.</li>
 *   <li>Вернуться в Explore через overflow-меню.</li>
 *   <li>Открыть вкладку «Сохранённые», скрыть промо (если есть), войти в дефолтный список и
 *       проверить наличие сохранённой статьи по заголовку.</li>
 * </ol>
 * </p>
 *
 * <p><b>EN:</b> UI test that searches an article, opens it, saves it, waits for a snackbar,
 * navigates to Saved tab, and verifies the article appears in the default Saved list.</p>
 */
@Slf4j
@Epic("Wikipedia (Android)")
@Feature("Saved / Сохранённые")
@Story("Добавление статьи в «Сохранённое»")
@Severity(SeverityLevel.CRITICAL)
@Owner("Alice Fabler")
@Tags({@Tag("android"), @Tag("local"), @Tag("remote"), @Tag("wikipedia")})
public class ArticleActionsTests extends TestBase {

    private final Variables vars = new Variables();
    private final DataExtractor data = new DataExtractor();

    /**
     * Сценарий: поиск → открыть первый результат → сохранить → подтвердить по Snackbar →
     * перейти во вкладку «Сохранённые» и убедиться, что статья присутствует в дефолтном списке.
     *
     * <p><b>EN:</b> Search → open first result → save → wait for snackbar →
     * go to Saved → assert article is present in the default list.</p>
     */
    @Test
    @DisplayName("Сохранённые: добавить статью из поиска и увидеть её в дефолтном списке")
    @Description("""
            Поиск «query» → открыть первый результат → «Сохранить» → дождаться snackbar →
            перейти в «Сохранённые» и убедиться, что статья присутствует в дефолтном списке.
            """)
    @Issue("HOMEWORK-1525")
    @AllureId("40937")
    void addArticleToSaved() {
        final String query = vars.randomSearchValue;
        parameter("query", query);

        step("Найти статью по запросу", () -> {
            step("Пропустить онбординг (если показан)", App.screens().onboarding::skipIfVisible);
            step("Открыть поиск с «Ленты»", App.screens().explore::openSearch);
            step("Ввести запрос: " + query, () -> App.screens().search.typeQuery(query));
            step("Открыть первый результат и запомнить заголовок",
                    () -> App.screens().search.openFirstResultAndRememberTitle(data));
        });

        step("Сохранить статью", () -> {
            step("Закрыть возможные оверлеи (инфо-плашки/попапы)", App.components().overlays::closeAllIfShown);
            step("Нажать «Сохранить»", App.screens().article::tapSave);
            step("Дождаться snackbar-подтверждения «…сохранено…»", App.components().snackbar::waitSavedConfirmation);
        });

        step("Вернуться на «Ленту» из «Статьи»", () -> {
            step("Открыть меню «Больше настроек»", App.screens().article::openOverflow);
            step("В меню выбрать «Лента»", App.screens().article::goToExplore);
        });

        step("Проверить наличие статьи в «Сохранённых»", () -> {
            step("Открыть вкладку «Сохранённые»", () -> App.components().bottomTabBar.openSaved());
            step("Закрыть промо «Еженедельный список…», если показан", App.screens().saved::dismissWeeklyPromoIfShown);
            step("Войти в дефолтный список «Сохранённое» (если требуется)", App.screens().saved::openDefaultReadingListIfNeeded);
            step("Закрыть возможные оверлеи (инфо-плашки/попапы)", App.components().overlays::closeAllIfShown);
            step("Убедиться, что присутствует статья: '" + data.getArticleName() + "'",
                    () -> App.screens().saved.shouldContainArticleTitled(data));
        });
    }
}

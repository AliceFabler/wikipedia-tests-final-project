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

@Slf4j
@Epic("Wikipedia (Android)")
@Feature("Saved / Сохранённые")
@Story("Добавление статьи в «Сохранённое»")
@Severity(SeverityLevel.CRITICAL)
@Owner("Alice Fabler")
@Tags({@Tag("android"), @Tag("local"), @Tag("remote"), @Tag("wikipedia")})
public class ArticleActionsTests extends TestBase {

    // --- Utils / Data holders ---
    private final Variables vars = new Variables();
    private final DataExtractor data = new DataExtractor();

    @Test
    @DisplayName("Сохранённые: добавить статью из поиска и увидеть её в дефолтном списке")
    @Description("""
            Поиск «query» → открыть первый результат → «Сохранить» → дождаться snackbar →
            перейти в «Сохранённые» и убедиться, что статья присутствует в дефолтном списке.
            """)
//    @Link(name = "Spec", url = "https://example.org/wiki-mobile/specs")
    @Issue("HOMEWORK-1525")
//    @TmsLink("TMS-5678")
//    @AllureId("PO-001")
    void addArticleToSaved() {
        final String query = vars.randomSearchValue; // например, "Java"
        parameter("query", query);

        // 1) Поиск
        step("Найти статью по запросу", () -> {
            step("Пропустить онбординг (если показан)", App.screens().onboarding::skipIfVisible);
            step("Открыть поиск с «Ленты»", App.screens().explore::openSearch);
            step("Ввести запрос: " + query, () -> App.screens().search.typeQuery(query));
            step("Открыть первый результат и запомнить заголовок",
                    () -> App.screens().search.openFirstResultAndRememberTitle(data));
        });

        // 2) Сохранение
        step("Сохранить статью", () -> {
            step("Закрыть возможные оверлеи (инфо-плашки/попапы)", App.components().overlays::closeAllIfShown);
            step("Нажать «Сохранить»", App.screens().article::tapSave);
            step("Дождаться snackbar-подтверждения «…сохранено…»", App.components().snackbar::waitSavedConfirmation);
        });

        // 3) Возврат на экран «Лента»
        step("Вернуться на «Ленту» из «Статьи»", () -> {
            step("Открыть меню «Больше настроек»", App.screens().article::openOverflow);
            step("В меню выбрать «Лента»", App.screens().article::goToExplore);
        });

        // 4) Проверка в «Сохранённых»
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

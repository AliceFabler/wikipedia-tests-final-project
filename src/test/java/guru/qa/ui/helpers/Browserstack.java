package guru.qa.ui.helpers;

import guru.qa.ui.config.AuthConfig;
import org.aeonbits.owner.ConfigFactory;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;

/**
 * Вспомогательный доступ к BrowserStack App Automate API.
 *
 * <p>Получает ссылку на видео сессии по её {@code sessionId}.
 * Аутентификация — basic auth с учётными данными из {@link AuthConfig}.</p>
 */
public class Browserstack {

    /**
     * Возвращает прямую ссылку на видео mp4 для указанной сессии BrowserStack.
     *
     * @param sessionId идентификатор сессии (automation_session.id)
     * @return значение поля {@code automation_session.video_url}
     * @throws AssertionError если HTTP-код ответа не 200
     */
    public static String videoUrl(String sessionId) {
        String url = format("https://api.browserstack.com/app-automate/sessions/%s.json", sessionId);
        AuthConfig auth = ConfigFactory.create(AuthConfig.class, System.getProperties());

        return given()
                .auth().basic(auth.getUserName(), auth.getKey())
                .when()
                .get(url)
                .then()
                .log().all()
                .statusCode(200)
                .extract().path("automation_session.video_url");
    }
}

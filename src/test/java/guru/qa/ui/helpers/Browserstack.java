package guru.qa.ui.helpers;

import guru.qa.ui.config.AuthConfig;
import org.aeonbits.owner.ConfigFactory;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;

public class Browserstack {


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
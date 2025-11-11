package guru.qa.ui.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
        "system:properties",
        "system:env",
        "classpath:${env}.properties",
        "classpath:auth.properties"
})
public interface AuthConfig extends Config {

    @Key("userName")
    @DefaultValue("lissarider1")
    String getUserName();

    @Key("key")
    @DefaultValue("zb7dtizcYQAAoSxS9mKv")
    String getKey();

    @Key("remoteUrl")
    @DefaultValue("http://hub.browserstack.com/wd/hub")
    String getRemoteUrl();
}

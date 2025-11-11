package guru.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Часть модели ответа /page/summary/{title}.
 * Поля сведены к минимуму, остальное игнорируем.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageSummary {

    private String title;
    private String description;
    private String extract;

    @JsonProperty("lang")
    private String language;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getExtract() {
        return extract;
    }

    public String getLanguage() {
        return language;
    }
}

package guru.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Модель части ответа для {@code GET /page/summary/{title}}.
 *
 * <p>Оставлены только ключевые поля, остальные игнорируются.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageSummary {

    /** Нормализованный заголовок страницы. */
    private String title;

    /** Краткое текстовое описание. Может отсутствовать. */
    private String description;

    /** Расширенная краткая выжимка по статье. */
    private String extract;

    /** Язык документа, возвращаемый API. */
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

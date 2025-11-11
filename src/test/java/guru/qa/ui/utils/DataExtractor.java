package guru.qa.ui.utils;

import lombok.Data;

/**
 * Контейнер для «протянутых» между шагами значений.
 * <p>Используется в тестах для запоминания введённого запроса и заголовка открытой статьи.</p>
 */
@Data
public class DataExtractor {
    /** Фактически введённая строка поиска (после масок/правок UI). */
    private String searchInput;
    /** Заголовок статьи, открытую из выдачи/карточки. */
    private String articleName;
}

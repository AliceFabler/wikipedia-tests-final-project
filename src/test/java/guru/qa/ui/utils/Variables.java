package guru.qa.ui.utils;

/**
 * Набор переменных сценария, «замораживаемых» на время теста.
 * <p>Создавайте новый экземпляр в каждом тесте, чтобы значения были независимы.</p>
 */
public class Variables {
    private final DataGenerator gen = new DataGenerator();

    /** Позитивный поисковый запрос (например, название страны). */
    public final String randomSearchValue = gen.getRandomSearchValue();
    /** Негативный запрос, с высокой вероятностью приводящий к пустой выдаче. */
    public final String negativeSearchValue = gen.getNonexistentQuery();
}

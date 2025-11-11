package guru.qa.ui.utils;

import net.datafaker.Faker;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Генератор тестовых данных для сценариев поиска.
 * <p>Использует {@link ThreadLocal}-Faker (потокобезопасно для параллельных прогонов).</p>
 */
public class DataGenerator {

    private static final ThreadLocal<Faker> TL_FAKER =
            ThreadLocal.withInitial(() -> new Faker(Locale.ENGLISH, ThreadLocalRandom.current()));

    /** Случайный «живой» запрос, который обычно даёт результаты (например, название страны). */
    public String getRandomSearchValue() {
        return TL_FAKER.get().country().name();
    }

    /** Гиббериш для негативных проверок (маловероятно, что даст результаты). */
    public String getRandomGibberish(int length) {
        final String alphabet = "qxzwvkjybhgfp"; // намеренно «редкие» буквы
        StringBuilder sb = new StringBuilder(Math.max(length, 1));
        for (int i = 0; i < length; i++) {
            int idx = ThreadLocalRandom.current().nextInt(alphabet.length());
            sb.append(alphabet.charAt(idx));
        }
        return sb.toString();
    }

    /** Негативный запрос по умолчанию (для тестов «пустой выдачи»). */
    public String getNonexistentQuery() {
        return "qxz-" + getRandomGibberish(8) + "-" + ThreadLocalRandom.current().nextInt(10_000, 99_999);
    }
}

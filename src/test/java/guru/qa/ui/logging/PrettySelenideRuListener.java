package guru.qa.ui.logging;

import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.LogEvent.EventStatus;
import com.codeborne.selenide.logevents.LogEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Красивый русифицированный логгер Selenide-событий.
 * Совместим с Selenide 7.x: реализует только beforeEvent/afterEvent.
 */
public class PrettySelenideRuListener implements LogEventListener {
    private static final Logger log = LoggerFactory.getLogger("Selenide");
    private static final int MAX_ELEM = 180;
    private static final Pattern SHOULD_RX =
            Pattern.compile("should\\s+(?<cond>.+)", Pattern.CASE_INSENSITIVE);

    @Override
    public void beforeEvent(LogEvent e) {
        String action = toRuAction(e);
        String elem = concise(e.getElement());
        if (!elem.isEmpty()) {
            log.info("➡️  {} | Элемент: {}", action, elem);
        } else {
            log.info("➡️  {}", action);
        }
    }

    @Override
    public void afterEvent(LogEvent e) {
        String action = toRuAction(e);
        long ms = e.getDuration();
        if (e.getStatus() == EventStatus.PASS) {
            log.info("✅ Успех: {} — {} мс", action, ms);
        } else if (e.getStatus() == EventStatus.FAIL) {
            String err = e.getError() != null ? e.getError().getMessage() : "неизвестная ошибка";
            log.error("❌ Ошибка: {} — {} ({} мс)", action, err, ms, e.getError());
        } else {
            log.warn("⚠️  Статус: {} — {} мс", action, ms);
        }
    }

    // --- helpers ---

    private String toRuAction(LogEvent e) {
        String s = (e.getSubject() == null ? "" : e.getSubject()).trim();
        String low = s.toLowerCase(Locale.ROOT);

        if (low.startsWith("click")) return "Клик";
        if (low.contains("double") && low.contains("click")) return "Двойной клик";
        if (low.contains("context") && low.contains("click")) return "Контекстный клик";
        if (low.startsWith("set value") || low.startsWith("set")) return "Ввод текста";
        if (low.startsWith("send keys")) return "Отправка клавиш";
        if (low.startsWith("clear")) return "Очистка поля";
        if (low.startsWith("hover") || low.contains("mouse")) return "Наведение курсора";
        if (low.startsWith("press enter") || low.contains("press enter")) return "Нажатие Enter";
        if (low.startsWith("select")) return "Выбор значения";
        if (low.startsWith("open")) return "Открытие URL";
        if (low.startsWith("find")) return "Поиск элемента";
        if (low.startsWith("drag")) return "Перетаскивание";
        if (low.startsWith("execute")) return "Выполнение JavaScript";

        if (low.startsWith("should")) {
            Matcher m = SHOULD_RX.matcher(s);
            String cond = m.find() ? m.group("cond") : s;
            return "Ожидание: " + cond;
        }
        if (low.startsWith("get")) return "Чтение значения";
        if (low.startsWith("value")) return "Чтение значения";
        if (low.startsWith("text")) return "Чтение текста";
        if (low.startsWith("attribute")) return "Чтение атрибута";

        return "Действие: " + s;
    }

    private String concise(String element) {
        if (element == null || element.isBlank()) return "";
        String e = element.trim();
        return e.length() > MAX_ELEM ? e.substring(0, MAX_ELEM) + "…" : e;
    }
}

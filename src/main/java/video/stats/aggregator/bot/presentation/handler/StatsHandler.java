package video.stats.aggregator.bot.presentation.handler;

import video.stats.aggregator.bot.application.service.VideoService;
import video.stats.aggregator.bot.presentation.ui.MessageFormatter;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;

import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.buildStatsKeyboard;

public class StatsHandler implements UpdateHandler {
    private static final Logger log = LoggerFactory.getLogger(StatsHandler.class);

    private final VideoService service;
    private final BotMessenger messenger;

    public StatsHandler(VideoService service, BotMessenger messenger) {
        this.service = service;
        this.messenger = messenger;
    }

    @Override
    public void handle(long chatId, Integer msgId, String arg) throws TelegramApiException {
        try {
            if ("refresh".equals(arg)) {
                service.refreshAll();
            }

            long count = service.getTotalCount();
            long views = service.getTotalViews();
            String text = MessageFormatter.buildStatsMessage(count, views);
            messenger.sendOrEdit(chatId, msgId, text, buildStatsKeyboard());
        } catch (SQLException e) {
            log.error("DB error in StatsHandler", e);
            messenger.sendHtml(chatId, "❌ Ошибка базы данных.");
        }
    }
}

package video.stats.aggregator.bot.presentation.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import video.stats.aggregator.bot.application.service.VideoService;
import video.stats.aggregator.bot.presentation.ui.MessageFormatter;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

import java.sql.SQLException;

import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.buildStatsKeyboard;

public class StatsHandler extends BaseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(StatsHandler.class);
    private final VideoService service;

    public StatsHandler(VideoService service, BotMessenger messenger) {
        super(messenger);
        this.service = service;
    }

    @Override
    public void handle(long chatId, Integer msgId, String arg) throws TelegramApiException {
        try {
            if ("refresh".equals(arg)) {
                service.refreshAll();
            }
            messenger.sendOrEdit(chatId, msgId,
                    MessageFormatter.buildStatsMessage(service.getTotalCount(), service.getTotalViews()),
                    buildStatsKeyboard());
        } catch (SQLException e) {
            sendError(chatId, "Ошибка базы данных.", LOG, e);
        }
    }
}

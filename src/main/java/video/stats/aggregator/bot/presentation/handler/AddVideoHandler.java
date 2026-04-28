package video.stats.aggregator.bot.presentation.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import video.stats.aggregator.bot.application.service.VideoService;
import video.stats.aggregator.bot.domain.entity.Video;
import video.stats.aggregator.bot.presentation.ui.MessageFormatter;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

import java.sql.SQLException;

import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.buildAddedVideoKeyboard;
import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.buildBackToMenuKeyboard;

public class AddVideoHandler extends BaseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AddVideoHandler.class);
    private final VideoService service;

    public AddVideoHandler(VideoService service, BotMessenger messenger) {
        super(messenger);
        this.service = service;
    }

    @Override
    public void handle(long chatId, Integer msgId, String url) throws TelegramApiException {
        if (url == null || url.isBlank()) {
            messenger.sendOrEdit(chatId, msgId, MessageFormatter.buildAddInstructionsMessage(),
                    buildBackToMenuKeyboard());
            return;
        }

        messenger.sendHtml(chatId, "⏳ Добавляю ссылку и получаю статистику…");
        try {
            Video v = service.addVideo(url);
            messenger.sendOrEdit(chatId, null, MessageFormatter.buildVideoAddedMessage(v), buildAddedVideoKeyboard());
        } catch (IllegalArgumentException e) {
            messenger.sendOrEdit(chatId, null,
                    "❌ " + MessageFormatter.escapeHtml(e.getMessage()) + "\n\n<i>Поддерживаются: YouTube, RuTube</i>",
                    buildBackToMenuKeyboard());
        } catch (SQLException e) {
            sendError(chatId, "Ошибка базы данных. Попробуйте позже.", LOG, e);
        }
    }
}

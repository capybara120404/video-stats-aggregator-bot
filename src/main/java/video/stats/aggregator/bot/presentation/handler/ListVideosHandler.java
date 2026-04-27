package video.stats.aggregator.bot.presentation.handler;

import video.stats.aggregator.bot.application.service.VideoService;
import video.stats.aggregator.bot.domain.entity.Video;
import video.stats.aggregator.bot.presentation.ui.MessageFormatter;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.List;

import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.buildListKeyboard;

public class ListVideosHandler implements UpdateHandler {
    private static final Logger log = LoggerFactory.getLogger(ListVideosHandler.class);

    private final VideoService service;
    private final BotMessenger messenger;

    public ListVideosHandler(VideoService service, BotMessenger messenger) {
        this.service = service;
        this.messenger = messenger;
    }

    @Override
    public void handle(long chatId, Integer msgId, String arg) throws TelegramApiException {
        try {
            if ("refresh".equals(arg)) {
                service.refreshAll();
            }

            List<Video> videos = service.getAllVideos();
            String text = MessageFormatter.buildListMessage(videos);
            InlineKeyboardMarkup markup = buildListKeyboard(videos);
            messenger.sendOrEdit(chatId, msgId, text, markup);
        } catch (SQLException e) {
            log.error("DB error in ListVideosHandler", e);
            messenger.sendHtml(chatId, "❌ Ошибка базы данных.");
        }
    }
}

package video.stats.aggregator.bot.presentation.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import video.stats.aggregator.bot.application.service.VideoService;
import video.stats.aggregator.bot.domain.entity.Video;
import video.stats.aggregator.bot.presentation.ui.MessageFormatter;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

import java.sql.SQLException;
import java.util.List;

import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.buildListKeyboard;

public class ListVideosHandler extends BaseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ListVideosHandler.class);
    private final VideoService service;

    public ListVideosHandler(VideoService service, BotMessenger messenger) {
        super(messenger);
        this.service = service;
    }

    @Override
    public void handle(long chatId, Integer msgId, String arg) throws TelegramApiException {
        try {
            if ("refresh".equals(arg)) {
                service.refreshAll();
            }
            List<Video> videos = service.getAllVideos();
            messenger.sendOrEdit(chatId, msgId, MessageFormatter.buildListMessage(videos), buildListKeyboard(videos));
        } catch (SQLException e) {
            sendError(chatId, "Ошибка базы данных.", LOG, e);
        }
    }
}

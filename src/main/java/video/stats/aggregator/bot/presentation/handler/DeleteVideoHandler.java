package video.stats.aggregator.bot.presentation.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import video.stats.aggregator.bot.application.service.VideoService;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

import java.sql.SQLException;

import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.buildBackToMenuKeyboard;

public class DeleteVideoHandler extends BaseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteVideoHandler.class);
    private final VideoService service;
    private final ListVideosHandler listHandler;

    public DeleteVideoHandler(VideoService service, BotMessenger messenger, ListVideosHandler listHandler) {
        super(messenger);
        this.service = service;
        this.listHandler = listHandler;
    }

    @Override
    public void handle(long chatId, Integer msgId, String arg) throws TelegramApiException {
        if (arg == null || arg.isBlank()) {
            messenger.sendHtml(chatId, "❗ Укажите ID: <code>/delete 3</code>");
            return;
        }

        long id;
        try {
            id = Long.parseLong(arg);
        } catch (NumberFormatException e) {
            messenger.sendOrEdit(chatId, null, "❗ ID должен быть числом.", buildBackToMenuKeyboard());
            return;
        }

        try {
            if (service.deleteVideo(id)) {
                messenger.sendOrEdit(chatId, null, "✅ Видео #" + id + " удалено.", buildBackToMenuKeyboard());
                listHandler.handle(chatId, null, null);
            } else {
                messenger.sendOrEdit(chatId, null, "❓ Видео не найдено.", buildBackToMenuKeyboard());
            }
        } catch (SQLException e) {
            sendError(chatId, "Ошибка базы данных.", LOG, e);
        }
    }
}

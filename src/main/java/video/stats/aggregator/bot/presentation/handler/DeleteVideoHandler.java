package video.stats.aggregator.bot.presentation.handler;

import video.stats.aggregator.bot.application.service.VideoService;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;

import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.buildBackToMenuKeyboard;

public class DeleteVideoHandler implements UpdateHandler {
    private static final Logger log = LoggerFactory.getLogger(DeleteVideoHandler.class);

    private final VideoService service;
    private final BotMessenger messenger;
    private final ListVideosHandler listHandler;

    public DeleteVideoHandler(VideoService service, BotMessenger messenger, ListVideosHandler listHandler) {
        this.service = service;
        this.messenger = messenger;
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
            messenger.sendOrEdit(chatId, null, "❗ ID должен быть числом. Используйте /list чтобы узнать ID.",
                    buildBackToMenuKeyboard());
            return;
        }

        try {
            boolean deleted = service.deleteVideo(id);
            if (deleted) {
                messenger.sendOrEdit(chatId, null, "✅ Видео #" + id + " удалено из списка.", buildBackToMenuKeyboard());
                listHandler.handle(chatId, null, null);
            } else {
                messenger.sendOrEdit(chatId, null, "❓ Видео с ID " + id + " не найдено.", buildBackToMenuKeyboard());
            }
        } catch (SQLException e) {
            log.error("DB error in DeleteVideoHandler", e);
            messenger.sendOrEdit(chatId, null, "❌ Ошибка базы данных.", buildBackToMenuKeyboard());
        }
    }
}

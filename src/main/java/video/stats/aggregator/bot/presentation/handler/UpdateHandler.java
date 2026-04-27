package video.stats.aggregator.bot.presentation.handler;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface UpdateHandler {
    void handle(long chatId, Integer messageId, String commandArgument) throws TelegramApiException;
}
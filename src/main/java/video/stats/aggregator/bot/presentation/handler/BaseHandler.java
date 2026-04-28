package video.stats.aggregator.bot.presentation.handler;

import org.slf4j.Logger;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

public abstract class BaseHandler implements UpdateHandler {
    protected final BotMessenger messenger;

    protected BaseHandler(BotMessenger messenger) {
        this.messenger = messenger;
    }

    protected void sendError(long chatId, String message, Logger log, Exception e) throws TelegramApiException {
        if (e != null) {
            log.error("Error in {}", this.getClass().getSimpleName(), e);
        }
        messenger.sendHtml(chatId, "❌ " + message);
    }
}

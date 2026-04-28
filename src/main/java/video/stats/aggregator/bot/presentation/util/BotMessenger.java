package video.stats.aggregator.bot.presentation.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BotMessenger {
    private static final Logger LOG = LoggerFactory.getLogger(BotMessenger.class);
    private final AbsSender sender;

    public BotMessenger(AbsSender sender) {
        this.sender = sender;
    }

    public void sendOrEdit(long chatId, Integer msgId, String text, InlineKeyboardMarkup markup)
            throws TelegramApiException {
        if (msgId != null) {
            try {
                sender.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(msgId)
                        .text(text)
                        .parseMode("HTML")
                        .replyMarkup(markup)
                        .disableWebPagePreview(true)
                        .build());
            } catch (TelegramApiException e) {
                LOG.warn("Failed to edit message {}: {}. Sending as new.", msgId, e.getMessage());
                sendNewMessage(chatId, text, markup);
            }
        } else {
            sendNewMessage(chatId, text, markup);
        }
    }

    private void sendNewMessage(long chatId, String text, InlineKeyboardMarkup markup)
            throws TelegramApiException {
        sender.execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(markup)
                .disableWebPagePreview(true)
                .build());
    }

    public void sendHtml(long chatId, String html) throws TelegramApiException {
        sender.execute(SendMessage.builder()
                .chatId(chatId)
                .text(html)
                .parseMode("HTML")
                .disableWebPagePreview(true)
                .build());
    }
}

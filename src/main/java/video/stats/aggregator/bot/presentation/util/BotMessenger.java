package video.stats.aggregator.bot.presentation.util;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BotMessenger {
    private final AbsSender sender;

    public BotMessenger(AbsSender sender) {
        this.sender = sender;
    }

    public void sendOrEdit(long chatId, Integer msgId, String text, InlineKeyboardMarkup markup)
            throws TelegramApiException {
        if (msgId != null) {
            sender.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(msgId)
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(markup)
                    .disableWebPagePreview(true)
                    .build());
        } else {
            sender.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(markup)
                    .disableWebPagePreview(true)
                    .build());
        }
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

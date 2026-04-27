package video.stats.aggregator.bot.presentation.handler;

import video.stats.aggregator.bot.presentation.ui.MessageFormatter;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.buildBackToMenuKeyboard;

public class HelpHandler implements UpdateHandler {
    private final BotMessenger messenger;

    public HelpHandler(BotMessenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public void handle(long chatId, Integer msgId, String arg) throws TelegramApiException {
        messenger.sendOrEdit(chatId, msgId, MessageFormatter.buildHelpMessage(), buildBackToMenuKeyboard());
    }
}

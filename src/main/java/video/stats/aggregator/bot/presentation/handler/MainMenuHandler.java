package video.stats.aggregator.bot.presentation.handler;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import video.stats.aggregator.bot.presentation.ui.MessageFormatter;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.buildMainMenuKeyboard;

public class MainMenuHandler extends BaseHandler {
    public MainMenuHandler(BotMessenger messenger) {
        super(messenger);
    }

    @Override
    public void handle(long chatId, Integer msgId, String arg) throws TelegramApiException {
        messenger.sendOrEdit(chatId, msgId, MessageFormatter.buildWelcomeMessage(), buildMainMenuKeyboard());
    }
}

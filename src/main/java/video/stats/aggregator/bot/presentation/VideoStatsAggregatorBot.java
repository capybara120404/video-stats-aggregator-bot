package video.stats.aggregator.bot.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import video.stats.aggregator.bot.application.service.VideoService;
import video.stats.aggregator.bot.domain.config.Config;
import video.stats.aggregator.bot.presentation.handler.AddVideoHandler;
import video.stats.aggregator.bot.presentation.handler.DeleteVideoHandler;
import video.stats.aggregator.bot.presentation.handler.HelpHandler;
import video.stats.aggregator.bot.presentation.handler.ListVideosHandler;
import video.stats.aggregator.bot.presentation.handler.MainMenuHandler;
import video.stats.aggregator.bot.presentation.handler.StatsHandler;
import video.stats.aggregator.bot.presentation.util.BotMessenger;

import static video.stats.aggregator.bot.presentation.ui.KeyboardFactory.*;

public class VideoStatsAggregatorBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(VideoStatsAggregatorBot.class);

    private final Config config;
    private final BotMessenger messenger;

    private final MainMenuHandler mainMenuHandler;
    private final HelpHandler helpHandler;
    private final ListVideosHandler listHandler;
    private final StatsHandler statsHandler;
    private final AddVideoHandler addHandler;
    private final DeleteVideoHandler deleteHandler;

    public VideoStatsAggregatorBot(Config config, VideoService service) {
        super(config.getBotToken());
        this.config = config;
        this.messenger = new BotMessenger(this);

        this.mainMenuHandler = new MainMenuHandler(messenger);
        this.helpHandler = new HelpHandler(messenger);
        this.listHandler = new ListVideosHandler(service, messenger);
        this.statsHandler = new StatsHandler(service, messenger);
        this.addHandler = new AddVideoHandler(service, messenger);
        this.deleteHandler = new DeleteVideoHandler(service, messenger, listHandler);
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Unhandled exception in update handler", e);
        }
    }

    private void handleMessage(Message msg) throws TelegramApiException {
        String text = msg.getText().trim();
        long chatId = msg.getChatId();
        String[] parts = text.split("\\s+", 2);
        String command = parts[0].toLowerCase();

        if (command.contains("@"))
            command = command.substring(0, command.indexOf('@'));

        switch (command) {
            case "/start" -> mainMenuHandler.handle(chatId, null, null);
            case "/help" -> helpHandler.handle(chatId, null, null);
            case "/list" -> listHandler.handle(chatId, null, null);
            case "/stats" -> statsHandler.handle(chatId, null, null);
            case "/add" -> {
                String arg = parts.length > 1 ? parts[1].trim() : null;
                addHandler.handle(chatId, null, arg);
            }
            case "/delete" -> {
                String arg = parts.length > 1 ? parts[1].trim() : null;
                deleteHandler.handle(chatId, null, arg);
            }
            default -> {
                if (text.startsWith("http://") || text.startsWith("https://") || text.contains("youtu")) {
                    addHandler.handle(chatId, null, text);
                } else if (command.startsWith("/del_")) {
                    deleteHandler.handle(chatId, null, command.substring(5));
                } else {
                    messenger.sendHtml(chatId, "❓ Неизвестная команда. Используйте меню ниже.");
                    mainMenuHandler.handle(chatId, null, null);
                }
            }
        }
    }

    private void handleCallback(CallbackQuery cb) throws TelegramApiException {
        String data = cb.getData();
        long chatId = cb.getMessage().getChatId();
        int msgId = cb.getMessage().getMessageId();

        String ackText = null;
        if (CB_REFRESH_ALL.equals(data) || CB_REFRESH_STATS.equals(data) || data.startsWith(CB_REFRESH_PFX)) {
            ackText = "⏳ Обновляю статистику…";
        }

        execute(AnswerCallbackQuery.builder()
                .callbackQueryId(cb.getId())
                .text(ackText)
                .build());

        if (data == null)
            return;

        switch (data) {
            case CB_MENU_MAIN -> mainMenuHandler.handle(chatId, msgId, null);
            case CB_MENU_LIST -> listHandler.handle(chatId, msgId, null);
            case CB_MENU_STATS -> statsHandler.handle(chatId, msgId, null);
            case CB_MENU_HELP -> helpHandler.handle(chatId, msgId, null);
            case CB_MENU_ADD -> addHandler.handle(chatId, msgId, null);
            case CB_REFRESH_ALL -> listHandler.handle(chatId, msgId, "refresh");
            case CB_REFRESH_STATS -> statsHandler.handle(chatId, msgId, "refresh");
            default -> {
                if (data.startsWith(CB_REFRESH_PFX)) {
                    listHandler.handle(chatId, msgId, "refresh");
                } else if (data.startsWith(CB_DELETE_PFX)) {
                    deleteHandler.handle(chatId, msgId, data.substring(CB_DELETE_PFX.length()));
                }
            }
        }
    }
}

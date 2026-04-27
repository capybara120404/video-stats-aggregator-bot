package video.stats.aggregator.bot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import video.stats.aggregator.bot.config.AppConfig;
import video.stats.aggregator.bot.model.Video;
import video.stats.aggregator.bot.model.VideoStatus;
import video.stats.aggregator.bot.service.VideoService;

import java.sql.SQLException;
import java.util.List;

public class VideoStatsAggregatorBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(VideoStatsAggregatorBot.class);

    private static final String CB_REFRESH_ALL = "refresh_all";
    private static final String CB_REFRESH_PFX = "refresh_";

    private final AppConfig config;
    private final VideoService service;

    public VideoStatsAggregatorBot(AppConfig config, VideoService service) {
        super(config.getBotToken());
        this.config = config;
        this.service = service;
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
            case "/start" -> sendHtml(chatId, buildWelcomeMessage());
            case "/help" -> sendHtml(chatId, buildHelpMessage());
            case "/list" -> handleList(chatId);
            case "/stats" -> handleStats(chatId);
            case "/add" -> {
                if (parts.length < 2 || parts[1].isBlank()) {
                    sendHtml(chatId, "❗ Укажите ссылку: <code>/add https://youtube.com/watch?v=...</code>");
                } else {
                    handleAdd(chatId, parts[1].trim());
                }
            }
            case "/delete" -> {
                if (parts.length < 2 || parts[1].isBlank()) {
                    sendHtml(chatId, "❗ Укажите ID: <code>/delete 3</code>");
                } else {
                    handleDelete(chatId, parts[1].trim());
                }
            }
            default -> {
                if (text.startsWith("http://") || text.startsWith("https://") || text.contains("youtu")) {
                    handleAdd(chatId, text);
                } else {
                    sendHtml(chatId, "❓ Неизвестная команда. Введите /help для справки.");
                }
            }
        }
    }

    private void handleAdd(long chatId, String url) throws TelegramApiException {
        sendHtml(chatId, "⏳ Добавляю ссылку и получаю статистику…");
        try {
            Video v = service.addVideo(url);
            sendHtml(chatId, buildVideoAddedMessage(v));
        } catch (IllegalArgumentException e) {
            sendHtml(chatId, "❌ " + escapeHtml(e.getMessage())
                    + "\n\n<i>Поддерживаются: YouTube, RuTube</i>");
        } catch (SQLException e) {
            log.error("DB error on add", e);
            sendHtml(chatId, "❌ Ошибка базы данных. Попробуйте позже.");
        }
    }

    private void handleList(long chatId) throws TelegramApiException {
        try {
            List<Video> videos = service.getAllVideos();
            String text = buildListMessage(videos);
            SendMessage msg = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(buildRefreshKeyboard())
                    .disableWebPagePreview(true)
                    .build();
            execute(msg);
        } catch (SQLException e) {
            log.error("DB error on list", e);
            sendHtml(chatId, "❌ Ошибка базы данных.");
        }
    }

    private void handleStats(long chatId) throws TelegramApiException {
        try {
            long count = service.getTotalCount();
            long views = service.getTotalViews();
            String text = "📊 <b>Общая статистика</b>\n\n"
                    + "🔗 Ссылок отслеживается: <b>" + count + "</b>\n"
                    + "👁 Суммарно просмотров:  <b>" + fmt(views) + "</b>";
            sendHtml(chatId, text);
        } catch (SQLException e) {
            log.error("DB error on stats", e);
            sendHtml(chatId, "❌ Ошибка базы данных.");
        }
    }

    private void handleDelete(long chatId, String arg) throws TelegramApiException {
        long id;
        try {
            id = Long.parseLong(arg);
        } catch (NumberFormatException e) {
            sendHtml(chatId, "❗ ID должен быть числом. Используйте /list чтобы узнать ID.");
            return;
        }
        try {
            boolean deleted = service.deleteVideo(id);
            if (deleted) {
                sendHtml(chatId, "✅ Видео #" + id + " удалено из списка.");
            } else {
                sendHtml(chatId, "❓ Видео с ID " + id + " не найдено.");
            }
        } catch (SQLException e) {
            log.error("DB error on delete", e);
            sendHtml(chatId, "❌ Ошибка базы данных.");
        }
    }

    private void handleCallback(CallbackQuery cb) throws TelegramApiException {
        String data = cb.getData();
        long chatId = cb.getMessage().getChatId();
        int msgId = cb.getMessage().getMessageId();

        execute(AnswerCallbackQuery.builder()
                .callbackQueryId(cb.getId())
                .text("⏳ Обновляю статистику…")
                .build());

        if (CB_REFRESH_ALL.equals(data)) {
            refreshAll(chatId, msgId);
        } else if (data != null && data.startsWith(CB_REFRESH_PFX)) {
            refreshAll(chatId, msgId);
        }
    }

    private void refreshAll(long chatId, int msgId) throws TelegramApiException {
        try {
            List<Video> updated = service.refreshAll();
            String newText = buildListMessage(updated);

            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(msgId)
                    .text(newText)
                    .parseMode("HTML")
                    .replyMarkup(buildRefreshKeyboard())
                    .disableWebPagePreview(true)
                    .build();
            execute(edit);
        } catch (SQLException e) {
            log.error("DB error on refresh", e);
            sendHtml(chatId, "❌ Ошибка при обновлении статистики.");
        }
    }

    private String buildWelcomeMessage() {
        return """
                👋 <b>Привет! Я бот для отслеживания просмотров видео.</b>

                Я умею следить за количеством просмотров на <b>YouTube</b> и <b>RuTube</b>.

                Просто отправь мне ссылку на видео — или используй команды:

                /add &lt;ссылка&gt; — добавить видео
                /list              — список всех видео
                /stats             — общая статистика
                /help              — справка
                """;
    }

    private String buildHelpMessage() {
        return """
                📖 <b>Справка по командам</b>

                <b>/add &lt;ссылка&gt;</b>
                Добавить видео для отслеживания.
                Поддерживаются: YouTube, RuTube.

                <b>/list</b>
                Показать все отслеживаемые видео с актуальными просмотрами.
                Используйте кнопку 🔄 для обновления статистики.

                <b>/stats</b>
                Показать общее количество ссылок и суммарные просмотры.

                <b>/delete &lt;id&gt;</b>
                Удалить видео из списка по его ID (ID виден в /list).

                <b>Можно просто отправить ссылку</b> — она будет добавлена автоматически.

                <i>Статусы:</i>
                ✅ Ок — данные актуальны
                ⚠️ Недоступно — платформа не отвечает, показаны последние данные
                🔑 Нет API-ключа — не настроен ключ доступа
                🚫 Не найдено — видео удалено или закрыто
                ❌ Ошибка — прочие ошибки
                """;
    }

    private String buildVideoAddedMessage(Video v) {
        boolean isNew = v.getLastUpdated() != null
                || v.getStatus() == VideoStatus.PENDING;

        StringBuilder sb = new StringBuilder();
        if (v.getAddedAt() != null && v.getLastUpdated() == null
                && v.getStatus() == VideoStatus.PENDING) {
            sb.append("ℹ️ Видео уже есть в списке.\n\n");
        } else {
            sb.append("✅ <b>Видео добавлено!</b>\n\n");
        }

        sb.append(v.getPlatform().format()).append("\n");
        sb.append("📌 <a href=\"").append(v.getUrl()).append("\">")
                .append(escapeHtml(v.getDisplayTitle())).append("</a>\n");
        sb.append("👁 ").append(v.getFormattedViews()).append(" просмотров\n");
        sb.append("📊 ").append(v.getStatus().getLabel());

        if (v.getStatus() == VideoStatus.UNAVAILABLE || v.getStatus() == VideoStatus.ERROR) {
            sb.append("\n<i>").append(escapeHtml(v.getErrorMessage())).append("</i>");
        }

        sb.append("\n\n<i>Используйте /list чтобы увидеть все видео.</i>");
        return sb.toString();
    }

    private String buildListMessage(List<Video> videos) {
        if (videos.isEmpty()) {
            return "📋 <b>Список пуст.</b>\n\nДобавьте видео командой /add &lt;ссылка&gt;";
        }

        long totalViews = videos.stream().mapToLong(Video::getViews).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("📋 <b>Отслеживаемые видео</b> (").append(videos.size()).append("):\n\n");

        for (int i = 0; i < videos.size(); i++) {
            Video v = videos.get(i);
            sb.append("<b>").append(i + 1).append(".</b> ")
                    .append(v.getPlatform().format())
                    .append("  <code>#").append(v.getId()).append("</code>\n");

            sb.append("📌 <a href=\"").append(v.getUrl()).append("\">")
                    .append(escapeHtml(v.getDisplayTitle())).append("</a>\n");

            sb.append("👁 <b>").append(v.getFormattedViews()).append("</b> просмотров\n");
            sb.append("📊 ").append(v.getStatus().getLabel());

            if (v.getStatus() == VideoStatus.UNAVAILABLE) {
                sb.append(" <i>(последние известные данные)</i>");
            }

            sb.append("\n🕐 ").append(v.getFormattedLastUpdated()).append("\n\n");
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("📊 <b>Итого:</b> ").append(videos.size())
                .append(" видео | <b>").append(fmt(totalViews)).append("</b> просмотров");

        return sb.toString();
    }

    private InlineKeyboardMarkup buildRefreshKeyboard() {
        InlineKeyboardButton btn = InlineKeyboardButton.builder()
                .text("🔄 Обновить статистику")
                .callbackData(CB_REFRESH_ALL)
                .build();
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(btn))
                .build();
    }

    private void sendHtml(long chatId, String html) throws TelegramApiException {
        execute(SendMessage.builder()
                .chatId(chatId)
                .text(html)
                .parseMode("HTML")
                .disableWebPagePreview(true)
                .build());
    }

    private static String fmt(long n) {
        return String.format("%,d", n).replace(',', ' ');
    }

    private static String escapeHtml(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

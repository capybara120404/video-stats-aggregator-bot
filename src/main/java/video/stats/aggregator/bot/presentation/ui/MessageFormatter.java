package video.stats.aggregator.bot.presentation.ui;

import video.stats.aggregator.bot.domain.entity.Video;
import video.stats.aggregator.bot.domain.entity.VideoStatus;

import java.util.List;

public class MessageFormatter {

    public static String buildWelcomeMessage() {
        return """
                👋 <b>Привет! Я бот для отслеживания просмотров видео.</b>

                Я умею следить за количеством просмотров на <b>YouTube</b> и <b>RuTube</b>.

                Просто отправь мне ссылку на видео или используй меню ниже:
                """;
    }

    public static String buildHelpMessage() {
        return """
                📖 <b>Справка по боту</b>

                <b>Как добавить видео?</b>
                Просто отправьте ссылку (YouTube или RuTube) в чат. Бот автоматически начнет отслеживание.

                <b>Как посмотреть список?</b>
                Нажмите кнопку <b>📋 Мои видео</b>. Там же можно обновить данные.

                <b>Как удалить видео?</b>
                Чтобы удалить видео, нажмите на команду <b>/del_ID</b> рядом с ним.

                <i>Статусы:</i>
                ✅ Ок - данные актуальны
                ⚠️ Недоступно - платформа не отвечает
                🚫 Не найдено - видео удалено или закрыто
                ❌ Ошибка - прочие ошибки
                """;
    }

    public static String buildVideoAddedMessage(Video video) {
        StringBuilder sb = new StringBuilder();
        if (video.isNewlyCreated()) {
            sb.append("✅ <b>Видео добавлено!</b>\n\n");
        } else {
            sb.append("ℹ️ <b>Видео уже есть в списке.</b>\n")
                    .append("<i>Данные обновлены до актуальных:</i>\n\n");
        }

        sb.append(video.getPlatform().format()).append("\n");
        sb.append("📌 <a href=\"").append(video.getUrl()).append("\">")
                .append(escapeHtml(video.getDisplayTitle())).append("</a>\n");
        sb.append("👁 ").append(video.getFormattedViews()).append(" просмотров\n");
        sb.append("📊 ").append(video.getStatus().getLabel());

        if (video.getStatus() == VideoStatus.UNAVAILABLE || video.getStatus() == VideoStatus.ERROR) {
            sb.append("\n<i>").append(escapeHtml(video.getErrorMessage())).append("</i>");
        }
        return sb.toString();
    }

    public static String buildListMessage(List<Video> videos) {
        if (videos.isEmpty()) {
            return "📋 <b>Список пуст.</b>\n\nДобавьте видео командой /add &lt;ссылка&gt;";
        }

        long totalViews = videos.stream().mapToLong(Video::getViews).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("📋 <b>Отслеживаемые видео</b> (").append(videos.size()).append("):\n\n");

        for (int i = 0; i < videos.size(); i++) {
            Video video = videos.get(i);
            sb.append("<b>").append(i + 1).append(".</b> ")
                    .append(video.getPlatform().format())
                    .append(" - 🗑 /del_").append(video.getId()).append("\n");
            sb.append("📌 <a href=\"").append(video.getUrl()).append("\">")
                    .append(escapeHtml(video.getDisplayTitle())).append("</a>\n");
            sb.append("👁 <b>").append(video.getFormattedViews()).append("</b> просмотров\n");
            sb.append("📊 ").append(video.getStatus().getLabel());

            if (video.getStatus() == VideoStatus.UNAVAILABLE) {
                sb.append(" <i>(последние известные данные)</i>");
            }
            sb.append("\n🕐 ").append(video.getFormattedLastUpdated()).append("\n\n");
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("📊 <b>Итого:</b> ").append(videos.size())
                .append(" видео | <b>").append(formatNumber(totalViews)).append("</b> просмотров\n\n");
        sb.append("💡 <i>Чтобы удалить видео, нажмите на команду <b>/del_ID</b> рядом с ним.</i>");

        return sb.toString();
    }

    public static String buildStatsMessage(long count, long views) {
        return "📊 <b>Общая статистика</b>\n\n"
                + "🔗 Ссылок отслеживается: <b>" + count + "</b>\n"
                + "👁 Суммарно просмотров:  <b>" + formatNumber(views) + "</b>";
    }

    public static String buildAddInstructionsMessage() {
        return "➕ <b>Как добавить видео</b>\n\n"
                + "Просто отправьте мне ссылку на видео в YouTube или RuTube.\n\n"
                + "Например:\n<code>https://www.youtube.com/watch?v=dQw4w9WgXcQ</code>";
    }

    public static String formatNumber(long number) {
        return String.format("%,d", number).replace(',', ' ');
    }

    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

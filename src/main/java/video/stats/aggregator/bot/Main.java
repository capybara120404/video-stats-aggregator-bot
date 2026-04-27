package video.stats.aggregator.bot;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import video.stats.aggregator.bot.bot.VideoStatsAggregatorBot;
import video.stats.aggregator.bot.config.AppConfig;
import video.stats.aggregator.bot.db.DatabaseManager;
import video.stats.aggregator.bot.repository.VideoRepository;
import video.stats.aggregator.bot.service.VideoService;
import video.stats.aggregator.bot.service.platform.PlatformClient;
import video.stats.aggregator.bot.service.platform.RuTubeClient;
import video.stats.aggregator.bot.service.platform.YouTubeClient;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("╔═════════════════════════════════════════════╗");
        log.info("║      Video Stats Aggregator Bot v1.0.0      ║");
        log.info("╚═════════════════════════════════════════════╝");

        AppConfig config = AppConfig.load();
        DatabaseManager dbManager = new DatabaseManager(config);
        dbManager.initialize();
        VideoRepository repo = new VideoRepository(dbManager);

        List<PlatformClient> clients = List.of(
                new YouTubeClient(config),
                new RuTubeClient(config));

        VideoService service = new VideoService(repo, clients);

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            VideoStatsAggregatorBot bot = new VideoStatsAggregatorBot(config, service);
            botsApi.registerBot(bot);
            log.info("Bot @{} is running. Press Ctrl+C to stop.", config.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("Failed to start Telegram bot", e);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.info("Shutting down Video Stats Aggregator Bot...")));
    }
}

package video.stats.aggregator.bot;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import video.stats.aggregator.bot.application.port.client.PlatformClient;
import video.stats.aggregator.bot.application.service.VideoService;
import video.stats.aggregator.bot.domain.config.Config;
import video.stats.aggregator.bot.infrastructure.api.RuTubeClient;
import video.stats.aggregator.bot.infrastructure.api.YouTubeClient;
import video.stats.aggregator.bot.infrastructure.persistence.DatabaseContext;
import video.stats.aggregator.bot.infrastructure.persistence.repository.VideoRepository;
import video.stats.aggregator.bot.presentation.VideoStatsAggregatorBot;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("╔═════════════════════════════════════════════╗");
        log.info("║      Video Stats Aggregator Bot v1.0.0      ║");
        log.info("╚═════════════════════════════════════════════╝");

        Config config = Config.load();
        DatabaseContext context = new DatabaseContext(config);
        context.initialize();
        VideoRepository repo = new VideoRepository(context);

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

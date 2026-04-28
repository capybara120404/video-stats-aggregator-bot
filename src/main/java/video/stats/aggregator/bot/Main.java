package video.stats.aggregator.bot;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.util.List;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        LOG.info("╔═════════════════════════════════════════════╗");
        LOG.info("║      Video Stats Aggregator Bot v1.0.0      ║");
        LOG.info("╚═════════════════════════════════════════════╝");

        Config config = Config.load();
        DatabaseContext context = new DatabaseContext(config);
        context.initialize();
        VideoRepository repo = new VideoRepository(context);

        HttpClient httpClient = HttpClient.newBuilder().build();
        ObjectMapper mapper = new ObjectMapper();

        List<PlatformClient> clients = List.of(
                new YouTubeClient(httpClient, mapper, config.getYoutubeApiKey()),
                new RuTubeClient(httpClient, mapper));

        VideoService service = new VideoService(repo, clients);

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            VideoStatsAggregatorBot bot = new VideoStatsAggregatorBot(config, service);
            botsApi.registerBot(bot);
            LOG.info("Bot @{} is running. Press Ctrl+C to stop.", config.getBotUsername());
        } catch (TelegramApiException e) {
            LOG.error("Failed to start Telegram bot", e);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down Video Stats Aggregator Bot...");
        }));
    }
}

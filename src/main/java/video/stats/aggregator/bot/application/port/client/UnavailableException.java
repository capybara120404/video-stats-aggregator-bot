package video.stats.aggregator.bot.application.port.client;

public class UnavailableException extends PlatformException {
    public UnavailableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

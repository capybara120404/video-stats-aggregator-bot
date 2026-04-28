package video.stats.aggregator.bot.application.port.client;

public class PlatformException extends Exception {
    public PlatformException(String msg) {
        super(msg);
    }

    public PlatformException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

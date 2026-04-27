package video.stats.aggregator.bot.service.platform;

import video.stats.aggregator.bot.model.Platform;

public interface PlatformClient {
    Platform getPlatform();

    VideoStats fetchStats(String videoId) throws PlatformException;

    record VideoStats(String title, long viewCount) {
    }

    class PlatformException extends Exception {
        public PlatformException(String msg) {
            super(msg);
        }

        public PlatformException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    class ApiException extends PlatformException {
        public ApiException(String msg) {
            super(msg);
        }
    }

    class UnavailableException extends PlatformException {
        public UnavailableException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    class NotFoundException extends PlatformException {
        public NotFoundException(String msg) {
            super(msg);
        }
    }
}

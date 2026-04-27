FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="VideoStatsAggregatorBot"
LABEL description="Telegram bot for tracking video view counts"

WORKDIR /app

COPY --from=build /build/target/video-stats-aggregator-bot.jar app.jar

RUN addgroup -S botgroup && adduser -S botuser -G botgroup
USER botuser

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD pgrep -f "app.jar" > /dev/null || exit 1

ENTRYPOINT ["java", "-Xms64m", "-Xmx256m", "-jar", "app.jar"]

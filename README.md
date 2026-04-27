# video-stats-aggregator-bot

Telegram-бот для отслеживания количества просмотров видео на **YouTube** и **RuTube**.  
Бэкенд написан на чистой Java 17 без Spring/Spring Boot. Данные хранятся в PostgreSQL.

---

## 📋 Возможности

| Функция | Описание |
| --------- | ---------- |
| `/add <ссылка>` | Добавить видео в список отслеживания |
| `/list` | Показать все видео с актуальными просмотрами |
| `/stats` | Суммарное количество ссылок и просмотров |
| `/delete <id>` | Удалить видео из списка |
| 🔄 Кнопка обновления | Обновить статистику прямо в сообщении (inline-кнопка) |
| Прямая отправка ссылки | Просто отправьте URL — бот добавит его автоматически |

**Поддерживаемые платформы:**

- 🎬 **YouTube** — официальный YouTube Data API v3
- 📹 **RuTube** — публичный API `rutube.ru`

**Обработка недоступности платформ:**  
Если платформа временно недоступна, бот сохраняет последние известные данные и помечает статус `⚠️ Недоступно`.

---

## 🗂️ Структура проекта

```text
video-stats-aggregator-bot/
├── src/main/java/video/stats/aggregator/bot
│   ├── Main.java                          # Точка входа, сборка зависимостей
│   ├── bot/
│   │   └── VideoStatsAggregatorBot.java   # Telegram-бот (команды, inline-кнопки)
│   ├── config/
│   │   └── AppConfig.java                 # Конфигурация из переменных окружения
│   ├── db/
│   │   └── DatabaseManager.java           # Пул соединений PostgreSQL + DDL
│   ├── model/
│   │   ├── Platform.java                  # Enum: YOUTUBE, RUTUBE
│   │   ├── Video.java                     # Модель видео
│   │   └── VideoStatus.java               # Enum статусов: OK, UNAVAILABLE, …
│   ├── repository/
│   │   └── VideoRepository.java           # CRUD-операции (чистый JDBC)
│   ├── service/
│   │   ├── VideoService.java              # Бизнес-логика
│   │   └── platform/
│   │       ├── PlatformClient.java        # Интерфейс + иерархия исключений
│   │       ├── YouTubeClient.java         # YouTube Data API v3
│   │       └── RuTubeClient.java          # RuTube API
│   └── util/
│       └── PlatformDetector.java          # Определение платформы и video ID из URL
├── src/main/resources/
│   └── logback.xml                        # Настройки логирования
├── Dockerfile                             # Многоэтапная сборка (Maven → JRE Alpine)
├── docker-compose.yml                     # Bot + PostgreSQL
├── .env.example                           # Шаблон переменных окружения
└── pom.xml                                # Maven-зависимости
```

---

## 🚀 Быстрый старт (Docker)

### 1. Клонируйте репозиторий

```bash
git clone https://github.com/capybara120404/video-stats-aggregator-bot.git
cd video-stats-aggregator-bot
```

### 2. Создайте `.env` из шаблона

```bash
cp .env.example .env
```

Откройте `.env` и заполните:

```env
BOT_TOKEN=123456789:AABBCCDDEEFFaabbccddeeff   # от @BotFather
YOUTUBE_API_KEY=AIzaSy...                         # от Google Cloud Console
```

### 3. Запустите через Docker Compose

```bash
docker compose up -d --build
```

Бот запустится вместе с PostgreSQL. Проверьте логи:

```bash
docker compose logs -f bot
```

### 4. Остановка

```bash
docker compose down          # остановить
docker compose down -v       # остановить + удалить данные БД
```

---

## ⚙️ Переменные окружения

| Переменная | Обязательна | По умолчанию | Описание |
| --- | :-----------: | --- | --- |
| `BOT_TOKEN` | ✅ | — | Токен бота от [@BotFather](https://t.me/BotFather) |
| `YOUTUBE_API_KEY` | Рек. | `""` | Ключ YouTube Data API v3 |
| `BOT_USERNAME` | — | `VideoStatsAggregatorBot` | Username бота (без @) |
| `DB_USER` | — | `postgres` | Пользователь PostgreSQL |
| `DB_PASSWORD` | — | `postgres` | Пароль PostgreSQL |
| `HTTP_TIMEOUT_SECONDS` | — | `10` | Таймаут HTTP-запросов к платформам |

---

## 🔑 Получение API-ключей

### Telegram Bot Token

1. Откройте [@BotFather](https://t.me/BotFather) в Telegram
2. Отправьте `/newbot` и следуйте инструкциям
3. Скопируйте полученный токен в `BOT_TOKEN`

### YouTube Data API v3

1. Откройте [Google Cloud Console](https://console.cloud.google.com/)
2. Создайте проект (или выберите существующий)
3. Перейдите в **APIs & Services → Library**
4. Найдите **YouTube Data API v3** и нажмите **Enable**
5. Перейдите в **APIs & Services → Credentials → Create Credentials → API key**
6. Скопируйте ключ в `YOUTUBE_API_KEY`

> ℹ️ Бесплатная квота: **10 000 units/день** (запрос статистики = 1 unit).
> При отсутствии ключа YouTube-видео добавляются, но просмотры не обновляются (`🔑 Нет API-ключа`).

---

## 💾 Схема базы данных

```sql
CREATE TABLE videos (
    id           BIGSERIAL PRIMARY KEY,
    url          TEXT        NOT NULL UNIQUE,   -- оригинальная ссылка
    platform     VARCHAR(50) NOT NULL,           -- YOUTUBE / RUTUBE
    video_id     VARCHAR(500),                   -- ID видео на платформе
    title        TEXT,                           -- название видео
    views        BIGINT      NOT NULL DEFAULT 0, -- количество просмотров
    status       VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- статус последнего обновления
    error_msg    TEXT,                           -- сообщение об ошибке (если есть)
    last_updated TIMESTAMPTZ,                    -- время последнего обновления
    added_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 🛠️ Локальная разработка (без Docker)

### Требования

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### Сборка

```bash
mvn clean package -DskipTests
```

### Запуск

```bash
export BOT_TOKEN="ваш_токен"
export YOUTUBE_API_KEY="ваш_ключ"
export DB_URL="jdbc:postgresql://localhost:5432/videobot"
export DB_USER="postgres"
export DB_PASSWORD="postgres"

java -jar target/video-stats-aggregator-bot.jar
```

---

## 📱 Примеры использования

**Добавление YouTube-видео:**

```text
/add https://www.youtube.com/watch?v=dQw4w9WgXcQ
```

**Добавление RuTube-видео:**

```text
/add https://rutube.ru/video/f855f645b7aa3ae93d4d2c498f7ceb91/
```

**Вывод списка:**

```text
/list
```

```text
📋 Отслеживаемые видео (2):

1. 🎬 YouTube  #1
📌 Never Gonna Give You Up
👁 1 500 000 000 просмотров
📊 ✅ Ок
🕐 23.04.2026 15:30

2. 📹 RuTube  #2
📌 Видео с RuTube
👁 45 678 просмотров
📊 ✅ Ок
🕐 23.04.2026 15:30

━━━━━━━━━━━━━━━━━━━━
📊 Итого: 2 видео | 1 500 045 678 просмотров

[🔄 Обновить статистику]
```

---

## 🧱 Технологии

| Компонент | Технология |
| ----------- | ----------- |
| Язык | Java 17 |
| Telegram API | [telegrambots 6.9.7.1](https://github.com/rubenlagus/TelegramBots) |
| База данных | PostgreSQL 15 |
| JDBC | Чистый JDBC (без ORM) |
| HTTP-клиент | `java.net.http.HttpClient` (встроен в JDK 11+) |
| JSON | Jackson 2.16 |
| Логирование | SLF4J + Logback |
| Сборка | Maven 3.9 |
| Контейнеризация | Docker + Docker Compose |

---

## ❓ Частые вопросы

**Q: Бот запустился, но не отвечает**  
A: Проверьте `BOT_TOKEN`. Убедитесь, что бот не запущен в другом месте (конфликт long-polling).

**Q: YouTube показывает "🔑 Нет API-ключа"**  
A: Заполните `YOUTUBE_API_KEY` в файле `.env` и перезапустите: `docker compose restart bot`.

**Q: RuTube показывает "⚠️ Недоступно"**  
A: RuTube использует неофициальный API, который может быть недоступен. Бот сохранит последние известные данные и повторит попытку при следующем обновлении.

**Q: Как посмотреть логи?**  
A: `docker compose logs -f bot`

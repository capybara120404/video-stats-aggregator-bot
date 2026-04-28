# video-stats-aggregator-bot

Telegram-бот для отслеживания количества просмотров видео на **YouTube** и **RuTube**.  
Бэкенд написан на чистой Java 17 с применением принципов **Clean Architecture**. Данные хранятся в PostgreSQL.

---

## 📋 Возможности

| Функция | Описание |
| --------- | ---------- |
| `/add <ссылка>` | Добавить видео в список отслеживания |
| `/list` | Показать все видео с актуальными просмотрами |
| `/stats` | Суммарное количество ссылок и просмотров |
| `/del_<id>` | Удалить видео из списка |
| 🔄 Кнопка обновления | Обновить статистику прямо в сообщении (inline-кнопка) |
| Прямая отправка ссылки | Просто отправьте URL - бот добавит его автоматически |

**Поддерживаемые платформы:**

- 🎬 **YouTube** - официальный YouTube Data API v3
- 📹 **RuTube** - публичный API `rutube.ru`

**Обработка недоступности платформ:**  
Если платформа временно недоступна, бот сохраняет последние известные данные и помечает статус `⚠️ Недоступно`.

---

## 🗂️ Архитектура и структура проекта

Проект организован в соответствии с принципами **чистой архитектуры (Clean Architecture)**:

```text
video-stats-aggregator-bot/
├── src/main/java/video/stats/aggregator/bot
│   ├── Main.java                          # Точка входа, ручное внедрение зависимостей (DI)
│   ├── application/                       # Прикладной слой (Use Cases)
│   │   ├── port/                          # Порты (интерфейсы клиентских API и репозиториев)
│   │   └── service/                       # Бизнес-логика (VideoService)
│   ├── domain/                            # Доменная модель
│   │   ├── config/                        # Конфигурация (Config)
│   │   └── entity/                        # Сущности (Video, Platform, VideoStatus)
│   ├── infrastructure/                    # Инфраструктурный слой (Адаптеры)
│   │   ├── api/                           # Интеграция с API (YouTubeClient, RuTubeClient)
│   │   ├── persistence/                   # Слой БД (DatabaseContext, VideoRepository)
│   │   └── util/                          # Утилиты (PlatformDetector)
│   └── presentation/                      # Слой представления (Telegram API)
│       ├── VideoStatsAggregatorBot.java   # Основной класс Telegram-бота
│       ├── handler/                       # Обработчики команд бота
│       ├── ui/                            # Фабрики клавиатур и форматтеры текста
│       └── util/                          # Утилиты отправки сообщений
├── docker-compose.yml                     # Инфраструктура (Bot + PostgreSQL)
├── Dockerfile                             # Оптимизированная сборка (Maven → JRE Alpine)
└── pom.xml                                # Зависимости сборки
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

Заполните ключи в `.env`:

```env
BOT_TOKEN=123456789:AABBCCDDEEFFaabbccddeeff   # от @BotFather
YOUTUBE_API_KEY=AIzaSy...                      # от Google Cloud Console
```

### 3. Запустите через Docker Compose

```bash
docker compose up -d --build
```

Проверьте логи:

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
| `BOT_TOKEN` | ✅ | - | Токен бота от [@BotFather](https://t.me/BotFather) |
| `YOUTUBE_API_KEY` | Рек. | `""` | Ключ YouTube Data API v3 |
| `BOT_USERNAME` | - | `VideoStatsAggregatorBot` | Username бота (без @) |
| `DB_USER` | - | `postgres` | Пользователь PostgreSQL |
| `DB_PASSWORD` | - | `postgres` | Пароль PostgreSQL |
| `HTTP_TIMEOUT_SECONDS` | - | `10` | Таймаут HTTP-запросов к платформам |

---

## 🔑 Получение API-ключей

1. **Telegram Bot Token:** в Telegram найдите [@BotFather](https://t.me/BotFather), отправьте `/newbot` и скопируйте токен.
2. **YouTube Data API v3:** в [Google Cloud Console](https://console.cloud.google.com/) создайте проект, включите `YouTube Data API v3` в разделе *Library* и сгенерируйте API-ключ в *Credentials*.  
   *(Без ключа видео добавляются, но статистика по просмотрам с YouTube собираться не будет)*

---

## 💾 Схема базы данных

```sql
CREATE TABLE IF NOT EXISTS videos (
    id           BIGSERIAL PRIMARY KEY,
    url          TEXT        NOT NULL UNIQUE,   -- оригинальная ссылка
    platform     VARCHAR(50) NOT NULL,           -- YOUTUBE / RUTUBE
    video_id     VARCHAR(500),                   -- ID видео на платформе
    title        TEXT,                           -- название видео
    views        BIGINT      NOT NULL DEFAULT 0, -- количество просмотров
    status       VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- статус обновления
    error_msg    TEXT,                           -- сообщение об ошибке
    last_updated TIMESTAMPTZ,                    -- время последнего обновления
    added_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_videos_platform ON videos (platform);
CREATE INDEX IF NOT EXISTS idx_videos_added_at ON videos (added_at DESC);
```

---

## 🛠️ Локальная разработка (без Docker)

**Требования:** Java 17+, Maven 3.8+, PostgreSQL 14+

Сборка:

```bash
mvn clean package -DskipTests
```

Запуск:

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

**Добавление видео:**

```text
/add https://www.youtube.com/watch?v=dQw4w9WgXcQ
/add https://rutube.ru/video/f855f645b7aa3ae93d4d2c498f7ceb91/
```

**Вывод списка (/list):**

```text
📋 Отслеживаемые видео (1):

1. 🎬 YouTube - 🗑 /del_1
📌 Обзор ПЕРВОЙ раскладушки БЕЗ СКЛАДКИ - ТОП! (https://www.youtube.com/watch?v=zA3qxFAcXlc&t=317s)
👁 330 438 просмотров
📊 ✅ Ок
🕐 28.04.2026 13:16

━━━━━━━━━━━━━━━━━━━━
📊 Итого: 1 видео | 330 438 просмотров

💡 Чтобы удалить видео, нажмите на команду /del_ID рядом с ним.
```

**Удаление видео (/del_id):**

```text
/del_1
✅ Видео #1 удалено.
```

---

## 🧱 Технологии

| Компонент | Технология |
| ----------- | ----------- |
| Язык | Java 17 |
| Архитектура | Clean Architecture |
| Telegram API | [telegrambots 6.9.7.1](https://github.com/rubenlagus/TelegramBots) |
| База данных | PostgreSQL 17 |
| JDBC | Чистый JDBC |
| HTTP-клиент | `java.net.http.HttpClient` |
| JSON | Jackson 2.16 |
| Логирование | SLF4J + Logback |
| Инфраструктура | Docker + Docker Compose |

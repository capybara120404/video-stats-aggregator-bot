package video.stats.aggregator.bot.presentation.ui;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import video.stats.aggregator.bot.domain.entity.Video;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {
        public static final String CALLBACK_REFRESH_ALL = "refresh_all";
        public static final String CALLBACK_REFRESH_STATS = "refresh_stats";
        public static final String CALLBACK_MENU_MAIN = "menu_main";
        public static final String CALLBACK_MENU_LIST = "menu_list";
        public static final String CALLBACK_MENU_STATS = "menu_stats";
        public static final String CALLBACK_MENU_HELP = "menu_help";
        public static final String CALLBACK_MENU_ADD = "menu_add";
        public static final String CALLBACK_REFRESH_PFX = "refresh_";
        public static final String CALLBACK_DELETE_PFX = "delete_";

        public static InlineKeyboardMarkup buildMainMenuKeyboard() {
                return InlineKeyboardMarkup.builder()
                                .keyboardRow(List.of(
                                                InlineKeyboardButton.builder().text("📋 Мои видео")
                                                                .callbackData(CALLBACK_MENU_LIST).build(),
                                                InlineKeyboardButton.builder().text("📊 Статистика")
                                                                .callbackData(CALLBACK_MENU_STATS).build()))
                                .keyboardRow(List.of(
                                                InlineKeyboardButton.builder().text("➕ Добавить")
                                                                .callbackData(CALLBACK_MENU_ADD).build(),
                                                InlineKeyboardButton.builder().text("📖 Справка")
                                                                .callbackData(CALLBACK_MENU_HELP).build()))
                                .build();
        }

        public static InlineKeyboardMarkup buildBackToMenuKeyboard() {
                return InlineKeyboardMarkup.builder()
                                .keyboardRow(List.of(
                                                InlineKeyboardButton.builder().text("⬅️ В главное меню")
                                                                .callbackData(CALLBACK_MENU_MAIN).build()))
                                .build();
        }

        public static InlineKeyboardMarkup buildAddedVideoKeyboard() {
                return InlineKeyboardMarkup.builder()
                                .keyboardRow(List.of(
                                                InlineKeyboardButton.builder().text("📋 Мои видео")
                                                                .callbackData(CALLBACK_MENU_LIST).build(),
                                                InlineKeyboardButton.builder().text("⬅️ В меню")
                                                                .callbackData(CALLBACK_MENU_MAIN).build()))
                                .build();
        }

        public static InlineKeyboardMarkup buildListKeyboard(List<Video> videos) {
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                if (!videos.isEmpty()) {
                        rows.add(List.of(
                                        InlineKeyboardButton.builder().text("🔄 Обновить всё")
                                                        .callbackData(CALLBACK_REFRESH_ALL).build()));
                }

                rows.add(List.of(
                                InlineKeyboardButton.builder().text("⬅️ В главное меню")
                                                .callbackData(CALLBACK_MENU_MAIN).build()));

                return InlineKeyboardMarkup.builder().keyboard(rows).build();
        }

        public static InlineKeyboardMarkup buildStatsKeyboard() {
                return InlineKeyboardMarkup.builder()
                                .keyboardRow(List.of(
                                                InlineKeyboardButton.builder().text("🔄 Обновить")
                                                                .callbackData(CALLBACK_REFRESH_STATS).build()))
                                .keyboardRow(List.of(
                                                InlineKeyboardButton.builder().text("⬅️ Назад")
                                                                .callbackData(CALLBACK_MENU_MAIN).build()))
                                .build();
        }
}
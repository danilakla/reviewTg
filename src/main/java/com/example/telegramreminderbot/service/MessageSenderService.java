package com.example.telegramreminderbot.service;

import com.example.telegramreminderbot.model.Reminder;
import com.example.telegramreminderbot.telegram.TelegramApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageSenderService {
    private final TelegramApiClient telegramApiClient;
    private final ReminderService reminderService;
    private Map<Long, String> userStates = new ConcurrentHashMap<>();
    private Map<Long, Reminder.ReminderFrequency> userFrequencies = new ConcurrentHashMap<>();

    public MessageSenderService(TelegramApiClient telegramApiClient, @Lazy ReminderService reminderService) {
        this.telegramApiClient = telegramApiClient;
        this.reminderService = reminderService;
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            telegramApiClient.sendMessage(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageWithDeleteButton(Long chatId, String text, Long reminderId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("Удалить напоминание");
        deleteButton.setCallbackData("delete_" + reminderId);
        row.add(deleteButton);

        keyboard.add(row);
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            telegramApiClient.sendMessage(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackData.startsWith("delete_")) {
            Long reminderId = Long.parseLong(callbackData.split("_")[1]);
            reminderService.deleteReminder(chatId, reminderId);
            sendMessage(chatId, "Напоминание удалено!");
        } else if (callbackData.startsWith("frequency_")) {
            String frequency = callbackData.split("_")[1];
            userFrequencies.put(chatId, Reminder.ReminderFrequency.valueOf(frequency.toUpperCase()));
            userStates.put(chatId, "WAITING_FOR_EVENT_NAME");
            sendMessage(chatId, "Введите название события:");
        }
    }

    private void handleCustomMessage(Long chatId, String messageText) {
        String state = userStates.get(chatId);
        if (state != null) {
            switch (state) {
                case "WAITING_FOR_EVENT_NAME":
                    userStates.put(chatId, "WAITING_FOR_TIME");
                    sendMessage(chatId, "Введите время в формате ЧЧ:ММ (например, 14:30):");
                    break;
                case "WAITING_FOR_TIME":
                    try {
                        LocalDateTime time = LocalDateTime.parse(messageText, DateTimeFormatter.ofPattern("HH:mm"));
                        Reminder.ReminderFrequency frequency = userFrequencies.get(chatId);
                        reminderService.createReminder(chatId, messageText, time, frequency);
                        sendMessage(chatId, "Напоминание создано!");
                        userStates.remove(chatId);
                        userFrequencies.remove(chatId);
                    } catch (Exception e) {
                        sendMessage(chatId, "Неверный формат времени. Попробуйте еще раз (ЧЧ:ММ):");
                    }
                    break;
                default:
                    sendMessage(chatId, "Я не понимаю эту команду. Используйте /help для просмотра доступных команд.");
            }
        } else {
            sendMessage(chatId, "Я не понимаю эту команду. Используйте /help для просмотра доступных команд.");
        }
    }
} 
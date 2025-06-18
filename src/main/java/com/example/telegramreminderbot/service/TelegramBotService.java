package com.example.telegramreminderbot.service;

import com.example.telegramreminderbot.model.Reminder;
import com.example.telegramreminderbot.telegram.TelegramApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TelegramBotService {
    private final ReminderService reminderService;
    private final MessageSenderService messageSenderService;
    private final TelegramApiClient telegramApiClient;
    private Map<Long, String> userStates = new ConcurrentHashMap<>();
    private Map<Long, Reminder.ReminderFrequency> userFrequencies = new ConcurrentHashMap<>();
    private Map<Long, String> eventNames = new ConcurrentHashMap<>();

    public void handleUpdate(Update update) {
        if (update.hasMessage()) {
            handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();

        if (text == null) {
            return;
        }

        if (text.startsWith("/")) {
            handleCommand(chatId, text);
        } else {
            handleCustomMessage(chatId, text);
        }
    }

    private void handleCommand(Long chatId, String command) {
        switch (command) {
            case "/start":
                sendWelcomeMessage(chatId);
                break;
            case "/help":
                sendHelpMessage(chatId);
                break;
            case "/createevent":
                sendCreateEventMessage(chatId);
                break;
            case "/listevents":
                sendListEvents(chatId);
                break;
            case "/deleteevent":
                sendDeleteEventMessage(chatId);
                break;
            case "/editevent":
                sendEditEventMessage(chatId);
                break;
            default:
                messageSenderService.sendMessage(chatId, "Неизвестная команда. Используйте /help для просмотра доступных команд.");
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        if (callbackData.startsWith("delete_")) {
            Long reminderId = Long.parseLong(callbackData.split("_")[1]);
            reminderService.deleteReminder(chatId, reminderId);
            messageSenderService.sendMessage(chatId, "Напоминание удалено!");
        } else if (callbackData.startsWith("frequency_")) {
            String frequency = callbackData.split("_")[1];
            userFrequencies.put(chatId, Reminder.ReminderFrequency.valueOf(frequency.toUpperCase()));
            userStates.put(chatId, "WAITING_FOR_EVENT_NAME");
            messageSenderService.sendMessage(chatId, "Введите название события:");
        }
    }

    private void handleCustomMessage(Long chatId, String messageText) {
        String state = userStates.get(chatId);
        if (state != null) {
            switch (state) {
                case "WAITING_FOR_EVENT_NAME":
                    eventNames.put(chatId, messageText);
                    userStates.put(chatId, "WAITING_FOR_TIME");
                    messageSenderService.sendMessage(chatId, "Введите время в формате ЧЧ:ММ (например, 14:30):");
                    break;
                case "WAITING_FOR_TIME":
                    try {
                        // Добавляем ведущие нули если нужно
                        String[] timeParts = messageText.split(":");
                        if (timeParts.length == 2) {
                            int hours = Integer.parseInt(timeParts[0]);
                            int minutes = Integer.parseInt(timeParts[1]);
                            String formattedTime = String.format("%02d:%02d", hours, minutes);
                            
                            LocalTime time = LocalTime.parse(formattedTime, DateTimeFormatter.ofPattern("HH:mm"));
                            LocalDateTime dateTime = LocalDateTime.now()
                                    .withHour(time.getHour())
                                    .withMinute(time.getMinute())
                                    .withSecond(0)
                                    .withNano(0);

                            Reminder.ReminderFrequency frequency = userFrequencies.get(chatId);
                            String eventName = eventNames.get(chatId);
                            reminderService.createReminder(chatId, eventName, dateTime, frequency);
                            messageSenderService.sendMessage(chatId, "Напоминание создано!");
                            
                            // Очищаем состояния
                            userStates.remove(chatId);
                            userFrequencies.remove(chatId);
                            eventNames.remove(chatId);
                        } else {
                            throw new DateTimeParseException("Invalid time format", messageText, 0);
                        }
                    } catch (Exception e) {
                        messageSenderService.sendMessage(chatId, "Неверный формат времени. Попробуйте еще раз (ЧЧ:ММ):");
                    }
                    break;
                default:
                    messageSenderService.sendMessage(chatId, "Я не понимаю эту команду. Используйте /help для просмотра доступных команд.");
            }
        } else {
            messageSenderService.sendMessage(chatId, "Я не понимаю эту команду. Используйте /help для просмотра доступных команд.");
        }
    }

    private void sendWelcomeMessage(Long chatId) {
        String welcomeMessage = "Добро пожаловать! Я бот для создания напоминаний.\n" +
                "Используйте /help для просмотра доступных команд.";
        messageSenderService.sendMessage(chatId, welcomeMessage);
    }

    private void sendHelpMessage(Long chatId) {
        String helpMessage = "Доступные команды:\n" +
                "/createevent - Создать новое напоминание\n" +
                "/listevents - Показать список напоминаний\n" +
                "/deleteevent - Удалить напоминание\n" +
                "/editevent - Изменить напоминание\n" +
                "/help - Показать это сообщение";
        messageSenderService.sendMessage(chatId, helpMessage);
    }

    private void sendCreateEventMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите частоту напоминания:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Reminder.ReminderFrequency frequency : Reminder.ReminderFrequency.values()) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(frequency.name());  // Используем имя enum вместо getDeclaringClass().getName()
            button.setCallbackData("frequency_" + frequency.name().toLowerCase());
            row.add(button);
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        
        try {
            telegramApiClient.sendMessage(message);
        } catch (TelegramApiException e) {
            messageSenderService.sendMessage(chatId, "Ошибка при отправке сообщения. Попробуйте еще раз.");
        }
    }

    private void sendListEvents(Long chatId) {
        List<Reminder> reminders = reminderService.getUserReminders(chatId);
        if (reminders.isEmpty()) {
            messageSenderService.sendMessage(chatId, "У вас нет активных напоминаний.");
            return;
        }

        StringBuilder message = new StringBuilder("Ваши напоминания:\n\n");
        for (Reminder reminder : reminders) {
            message.append(String.format("- %s (%s)\n", reminder.getEventName(), reminder.getFrequency().name()));
        }
        messageSenderService.sendMessage(chatId, message.toString());
    }

    private void sendDeleteEventMessage(Long chatId) {
        List<Reminder> reminders = reminderService.getUserReminders(chatId);
        if (reminders.isEmpty()) {
            messageSenderService.sendMessage(chatId, "У вас нет активных напоминаний для удаления.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите напоминание для удаления:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Reminder reminder : reminders) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(reminder.getEventName());
            button.setCallbackData("delete_" + reminder.getId());
            row.add(button);
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);
        
        try {
            telegramApiClient.sendMessage(message);
        } catch (TelegramApiException e) {
            messageSenderService.sendMessage(chatId, "Ошибка при отправке сообщения. Попробуйте еще раз.");
        }
    }

    private void sendEditEventMessage(Long chatId) {
        messageSenderService.sendMessage(chatId, "Функция редактирования напоминаний пока не реализована.");
    }
} 
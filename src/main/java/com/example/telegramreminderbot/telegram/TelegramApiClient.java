package com.example.telegramreminderbot.telegram;

import com.example.telegramreminderbot.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramApiClient extends TelegramLongPollingBot {
    private final TelegramBotService telegramBotService;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    public TelegramApiClient(@Lazy TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        telegramBotService.handleUpdate(update);
    }

    public void sendMessage(SendMessage message) throws TelegramApiException {
        execute(message);
    }
} 
package com.example.telegramreminderbot.config;

import com.example.telegramreminderbot.telegram.TelegramApiClient;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {
    private final TelegramApiClient telegramApiClient;
    private TelegramBotsApi telegramBotsApi;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(telegramApiClient);
        return telegramBotsApi;
    }

    @PreDestroy
    public void cleanup() {
        if (telegramBotsApi != null) {
            try {
                // Stop the bot session
                telegramApiClient.clearWebhook();
                // The session will be automatically cleaned up by Spring
            } catch (TelegramApiException e) {
                // Log error but don't throw as this is during shutdown
                e.printStackTrace();
            }
        }
    }
} 
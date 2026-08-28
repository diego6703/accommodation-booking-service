package dev.diego.accommodationbookingservice.service.impl;

import dev.diego.accommodationbookingservice.service.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelegramNotificationServiceImpl implements TelegramNotificationService {

    private final RestClient telegramRestClient;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String chatId;

    @Override
    public void sendMessage(String message) {
        record TelegramRequest(String chat_id, String text) {}

        telegramRestClient.post()
                .uri("/bot" + botToken + "/sendMessage")
                .body(new TelegramRequest(chatId, message))
                .retrieve()
                .toBodilessEntity();
    }
}

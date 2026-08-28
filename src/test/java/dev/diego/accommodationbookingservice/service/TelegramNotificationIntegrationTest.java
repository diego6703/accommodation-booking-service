package dev.diego.accommodationbookingservice.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@EnableWireMock({
        @ConfigureWireMock(port = 0, baseUrlProperties = "telegram.api.url")
})
class TelegramNotificationIntegrationTest {

    @Autowired
    private TelegramNotificationService notificationService;

    @Test
    void shouldSendTelegramNotificationSuccessfully() {
        stubFor(post(urlPathMatching("/bot.*?/sendMessage"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}")));

        notificationService.sendMessage("🟢 Test notification from integration test");

        verify(postRequestedFor(urlPathMatching("/bot.*?/sendMessage"))
                .withRequestBody(matchingJsonPath("$.text",
                        equalTo("🟢 Test notification from integration test"))));
    }
}

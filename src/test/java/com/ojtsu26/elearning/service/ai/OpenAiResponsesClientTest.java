package com.ojtsu26.elearning.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.config.AiTutorProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiResponsesClientTest {

    @Test
    void insufficientQuotaMapsToBillingMessageWithoutRetry() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> {
            calls.incrementAndGet();
            byte[] body = """
                    {"error":{"code":"insufficient_quota","type":"insufficient_quota","message":"quota exceeded"}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(429, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            AiTutorProperties properties = new AiTutorProperties();
            properties.setApiKey("test-key");
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            OpenAiResponsesClient client = new OpenAiResponsesClient(properties, new ObjectMapper());

            AiTutorUnavailableException exception = assertThrows(AiTutorUnavailableException.class,
                    () -> client.generate(new AiTutorPrompt("instructions", "input", 10)));

            assertEquals("AI Tutor OpenAI quota or billing credit is unavailable.", exception.getMessage());
            assertEquals(1, calls.get());
        } finally {
            server.stop(0);
        }
    }
}

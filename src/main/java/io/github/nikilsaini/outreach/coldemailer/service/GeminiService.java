package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.config.GeminiProperties;
import io.github.nikilsaini.outreach.coldemailer.dto.request.GeminiRequest;
import io.github.nikilsaini.outreach.coldemailer.dto.request.GeminiRequest.Content;
import io.github.nikilsaini.outreach.coldemailer.dto.request.GeminiRequest.GenerationConfig;
import io.github.nikilsaini.outreach.coldemailer.dto.request.GeminiRequest.Part;
import io.github.nikilsaini.outreach.coldemailer.dto.response.GeminiResponse;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class GeminiService {

  private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
  private static final String JSON_MIME_TYPE = "application/json";

  private final GeminiProperties properties;
  private final RestClient googleRestClient;
  private final ObjectMapper objectMapper;

  @SneakyThrows
  public List<String> generateFollowups(String subject, String initialBody, int count) {
    GeminiRequest request = buildRequest(subject, initialBody, count);

    String url = GEMINI_BASE_URL + properties.model() + ":generateContent?key=" + properties.apiKey();

    GeminiResponse response = googleRestClient.post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .retrieve()
        .body(GeminiResponse.class);

    String json = response.extractText();
    return Arrays.asList(objectMapper.readValue(json, String[].class));
  }

  private GeminiRequest buildRequest(String subject, String initialBody, int count) {
    String prompt = """
        You are an email assistant helping with cold email outreach.
        Based on the following initial cold email, generate %d follow-up emails.
        Rules:
        - Each follow-up should be progressively shorter and more direct
        - Maintain a professional but friendly tone
        - Reference the initial email subtly without being repetitive
        - Each follow-up should have a slightly different angle

        Initial email subject: %s
        Initial email body:
        %s

        Return ONLY a JSON array of %d strings, where each string is the body of a follow-up email.
        """.formatted(count, subject, initialBody, count);

    return new GeminiRequest(
        List.of(new Content(List.of(new Part(prompt)))),
        new GenerationConfig(JSON_MIME_TYPE)
    );
  }
}

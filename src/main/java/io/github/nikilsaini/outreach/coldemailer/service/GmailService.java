package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.dto.response.GmailSendResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailService {

  private static final String GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

  private final RestClient googleRestClient;

  public GmailSendResponse sendEmail(String accessToken, String from, String to, String subject, String body) {
    log.atDebug().setMessage("Sending initial email").addKeyValue("to", to).log();
    String raw = buildRfc2822Email(from, to, subject, body);
    return post(accessToken, Map.of("raw", raw));
  }

  public GmailSendResponse sendFollowup(String accessToken, String from, String to, String subject,
      String body, String threadId, String rootMessageId) {
    log.atDebug().setMessage("Sending follow-up").addKeyValue("to", to).addKeyValue("threadId", threadId).log();
    String raw = buildRfc2822Reply(from, to, subject, body, rootMessageId);
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("raw", raw);
    payload.put("threadId", threadId);
    return post(accessToken, payload);
  }

  private GmailSendResponse post(String accessToken, Map<String, String> body) {
    return googleRestClient.post()
        .uri(GMAIL_SEND_URL)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(GmailSendResponse.class);
  }

  private String buildRfc2822Email(String from, String to, String subject, String body) {
    String email = "From: " + from + "\r\n"
        + "To: " + to + "\r\n"
        + "Subject: " + subject + "\r\n"
        + "Content-Type: text/plain; charset=utf-8\r\n"
        + "\r\n"
        + body;
    return Base64.getUrlEncoder().encodeToString(email.getBytes(StandardCharsets.UTF_8));
  }

  private String buildRfc2822Reply(String from, String to, String subject, String body, String rootMessageId) {
    String replySubject = subject.startsWith("Re: ") ? subject : "Re: " + subject;
    String messageIdHeader = "<" + rootMessageId + ">";
    String email = "From: " + from + "\r\n"
        + "To: " + to + "\r\n"
        + "Subject: " + replySubject + "\r\n"
        + "In-Reply-To: " + messageIdHeader + "\r\n"
        + "References: " + messageIdHeader + "\r\n"
        + "Content-Type: text/plain; charset=utf-8\r\n"
        + "\r\n"
        + body;
    return Base64.getUrlEncoder().encodeToString(email.getBytes(StandardCharsets.UTF_8));
  }
}

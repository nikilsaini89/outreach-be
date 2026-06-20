package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.dto.mapper.FollowupMapper;
import io.github.nikilsaini.outreach.coldemailer.dto.response.FollowupResponse;
import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.repository.FollowupRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowupService {

  private final FollowupRepository followupRepository;
  private final GeminiService geminiService;

  public List<String> generateBodies(String subject, String initialBody, int count) {
    return geminiService.generateFollowups(subject, initialBody, count);
  }

  @Transactional
  public List<FollowupResponse> save(Campaign campaign, List<String> bodies, int gapDays, int preferredHour) {
    AtomicInteger sequence = new AtomicInteger(1);
    return followupRepository.saveAll(
        bodies.stream()
            .map(body -> {
              int seq = sequence.getAndIncrement();
              LocalDateTime scheduledAt = LocalDateTime.now()
                  .plusDays((long) seq * gapDays)
                  .withHour(preferredHour)
                  .withMinute(0)
                  .withSecond(0)
                  .withNano(0);
              return FollowupMapper.toEntity(campaign, body, seq, scheduledAt);
            })
            .toList()
    ).stream().map(FollowupMapper::toResponse).toList();
  }
}

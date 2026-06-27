package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.dto.mapper.FollowupMapper;
import io.github.nikilsaini.outreach.coldemailer.dto.response.FollowupResponse;
import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.entity.Followup;
import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;
import io.github.nikilsaini.outreach.coldemailer.enums.FollowupStatus;
import io.github.nikilsaini.outreach.coldemailer.repository.FollowupRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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

  @Transactional(readOnly = true)
  public List<FollowupResponse> getFollowUpsForCampaign(UUID campaignId) {
    return followupRepository.findByCampaignIdOrderBySequenceNumberAsc(campaignId).stream()
        .map(FollowupMapper::toResponse)
        .toList();
  }

  /** Follow-ups due to send now: PENDING, past their scheduled time, on an ACTIVE campaign. */
  public List<Followup> findDue() {
    return followupRepository.findDueFollowups(
        FollowupStatus.PENDING, LocalDateTime.now(), CampaignStatus.ACTIVE);
  }

  public void updateStatus(Followup followup, FollowupStatus status) {
    followup.setStatus(status);
    followupRepository.save(followup);
  }

  /** True while the campaign still has follow-ups waiting (PENDING) or in flight (PROCESSING). */
  public boolean hasOutstanding(UUID campaignId) {
    return followupRepository.countByCampaignIdAndStatusIn(
        campaignId, List.of(FollowupStatus.PENDING, FollowupStatus.PROCESSING)) > 0;
  }

  public boolean hasFailures(UUID campaignId) {
    return followupRepository.countByCampaignIdAndStatusIn(
        campaignId, List.of(FollowupStatus.FAILED)) > 0;
  }
}

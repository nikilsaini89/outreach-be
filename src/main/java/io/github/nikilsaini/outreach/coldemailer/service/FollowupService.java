package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.dto.event.FollowupGenerationPayload;
import io.github.nikilsaini.outreach.coldemailer.dto.mapper.FollowupMapper;
import io.github.nikilsaini.outreach.coldemailer.dto.response.FollowupResponse;
import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.entity.Followup;
import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;
import io.github.nikilsaini.outreach.coldemailer.enums.FollowupStatus;
import io.github.nikilsaini.outreach.coldemailer.repository.CampaignRepository;
import io.github.nikilsaini.outreach.coldemailer.repository.FollowupRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowupService {

  private final FollowupRepository followupRepository;
  private final CampaignRepository campaignRepository;
  private final GeminiService geminiService;

  /**
   * Saves GENERATING stubs immediately so the API can return a campaign with follow-up placeholders
   * while generation is happening asynchronously via Kafka.
   */
  @Transactional
  public List<FollowupResponse> saveStubs(Campaign campaign, int count, int gapDays, int preferredHour) {
    AtomicInteger sequence = new AtomicInteger(1);
    List<Followup> stubs = followupRepository.saveAll(
        java.util.stream.IntStream.rangeClosed(1, count)
            .mapToObj(i -> {
              int seq = sequence.getAndIncrement();
              LocalDateTime scheduledAt = scheduledTime(seq, gapDays, preferredHour);
              return FollowupMapper.toStubEntity(campaign, seq, scheduledAt);
            })
            .toList()
    );
    log.atDebug().setMessage("Saved GENERATING stubs")
        .addKeyValue("campaignId", campaign.getId())
        .addKeyValue("count", stubs.size())
        .log();
    return stubs.stream().map(FollowupMapper::toResponse).toList();
  }

  /**
   * Called by the Kafka consumer: generates bodies from Gemini and writes them into the stubs,
   * transitioning each from GENERATING → PENDING.
   */
  @Transactional
  public void generateAndApplyBodies(FollowupGenerationPayload payload) {
    List<Followup> stubs = followupRepository.findByCampaignIdAndStatus(
        payload.campaignId(), FollowupStatus.GENERATING);
    if (stubs.isEmpty()) {
      log.atWarn().setMessage("No GENERATING stubs found — skipping")
          .addKeyValue("campaignId", payload.campaignId())
          .log();
      return;
    }

    List<String> bodies = geminiService.generateFollowups(
        payload.subject(), payload.initialBody(), stubs.size());

    stubs.sort(java.util.Comparator.comparingInt(Followup::getSequenceNumber));
    for (int i = 0; i < stubs.size(); i++) {
      Followup stub = stubs.get(i);
      stub.setBody(bodies.get(i));
      stub.setStatus(FollowupStatus.PENDING);
    }
    followupRepository.saveAll(stubs);

    log.atInfo().setMessage("Follow-up generation complete")
        .addKeyValue("campaignId", payload.campaignId())
        .addKeyValue("count", stubs.size())
        .log();
  }

  /**
   * Called by the DLT handler and safety-net scheduler when generation fails permanently.
   * Marks all GENERATING stubs and the parent campaign FAILED.
   */
  @Transactional
  public void markGenerationFailed(UUID campaignId) {
    List<Followup> stubs = followupRepository.findByCampaignIdAndStatus(
        campaignId, FollowupStatus.GENERATING);
    stubs.forEach(f -> f.setStatus(FollowupStatus.FAILED));
    followupRepository.saveAll(stubs);

    campaignRepository.findById(campaignId).ifPresent(c -> {
      c.setStatus(CampaignStatus.FAILED);
      campaignRepository.save(c);
    });

    log.atWarn().setMessage("Marked followup generation as failed")
        .addKeyValue("campaignId", campaignId)
        .addKeyValue("stubs", stubs.size())
        .log();
  }

  @Transactional
  public List<FollowupResponse> save(Campaign campaign, List<String> bodies, int gapDays, int preferredHour) {
    AtomicInteger sequence = new AtomicInteger(1);
    return followupRepository.saveAll(
        bodies.stream()
            .map(body -> {
              int seq = sequence.getAndIncrement();
              return FollowupMapper.toEntity(campaign, body, seq, scheduledTime(seq, gapDays, preferredHour));
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

  public List<Followup> findDue() {
    return followupRepository.findDueFollowups(
        FollowupStatus.PENDING, LocalDateTime.now(), CampaignStatus.ACTIVE);
  }

  public void updateStatus(Followup followup, FollowupStatus status) {
    log.atDebug().setMessage("Follow-up status updated")
        .addKeyValue("followupId", followup.getId())
        .addKeyValue("status", status)
        .log();
    followup.setStatus(status);
    followupRepository.save(followup);
  }

  public boolean hasOutstanding(UUID campaignId) {
    return followupRepository.countByCampaignIdAndStatusIn(
        campaignId, List.of(FollowupStatus.GENERATING, FollowupStatus.PENDING, FollowupStatus.PROCESSING)) > 0;
  }

  public boolean hasFailures(UUID campaignId) {
    return followupRepository.countByCampaignIdAndStatusIn(
        campaignId, List.of(FollowupStatus.FAILED)) > 0;
  }

  @Transactional
  public void cancelPending(UUID campaignId) {
    List<Followup> pending = followupRepository.findByCampaignIdAndStatus(campaignId, FollowupStatus.PENDING);
    pending.forEach(f -> f.setStatus(FollowupStatus.CANCELLED));
    followupRepository.saveAll(pending);
    log.atInfo().setMessage("Pending follow-ups cancelled")
        .addKeyValue("campaignId", campaignId)
        .addKeyValue("count", pending.size())
        .log();
  }

  private static LocalDateTime scheduledTime(int seq, int gapDays, int preferredHour) {
    return LocalDateTime.now()
        .plusDays((long) seq * gapDays)
        .withHour(preferredHour)
        .withMinute(0)
        .withSecond(0)
        .withNano(0);
  }
}

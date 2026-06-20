package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.dto.mapper.FollowupMapper;
import io.github.nikilsaini.outreach.coldemailer.dto.response.FollowupResponse;
import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.exception.CampaignNotFoundException;
import io.github.nikilsaini.outreach.coldemailer.repository.CampaignRepository;
import io.github.nikilsaini.outreach.coldemailer.repository.FollowupRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowupService {

  private final CampaignRepository campaignRepository;
  private final FollowupRepository followupRepository;
  private final GeminiService geminiService;

  @Transactional
  public List<FollowupResponse> generateAndSave(UUID campaignId, int count) {
    Campaign campaign = campaignRepository.findById(campaignId)
        .orElseThrow(() -> new CampaignNotFoundException(campaignId));

    List<String> bodies = geminiService.generateFollowups(campaign.getSubject(), campaign.getInitialBody(), count);

    AtomicInteger sequence = new AtomicInteger(1);
    return followupRepository.saveAll(
        bodies.stream()
            .map(body -> FollowupMapper.toEntity(campaign, body, sequence.getAndIncrement()))
            .toList()
    ).stream().map(FollowupMapper::toResponse).toList();
  }
}

package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.dto.mapper.CampaignMapper;
import io.github.nikilsaini.outreach.coldemailer.dto.mapper.FollowupMapper;
import io.github.nikilsaini.outreach.coldemailer.dto.request.CreateCampaignRequest;
import io.github.nikilsaini.outreach.coldemailer.dto.response.CampaignResponse;
import io.github.nikilsaini.outreach.coldemailer.dto.response.FollowupResponse;
import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.entity.User;
import io.github.nikilsaini.outreach.coldemailer.exception.UserNotFoundException;
import io.github.nikilsaini.outreach.coldemailer.repository.CampaignRepository;
import io.github.nikilsaini.outreach.coldemailer.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampaignService {

  private final CampaignRepository campaignRepository;
  private final UserRepository userRepository;
  private final FollowupService followupService;

  @Transactional
  public CampaignResponse createWithFollowups(CreateCampaignRequest request) {
    User user = userRepository.findById(request.userId())
        .orElseThrow(() -> new UserNotFoundException(request.userId()));

    Campaign saved = campaignRepository.save(CampaignMapper.toEntity(request, user));

    List<FollowupResponse> followups = followupService.generateAndSave(saved.getId(), request.followupCount());
    return FollowupMapper.toCampaignResponse(saved, followups);
  }
}

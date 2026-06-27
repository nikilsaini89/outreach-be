package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.dto.mapper.CampaignMapper;
import io.github.nikilsaini.outreach.coldemailer.dto.mapper.FollowupMapper;
import io.github.nikilsaini.outreach.coldemailer.dto.request.CreateCampaignRequest;
import io.github.nikilsaini.outreach.coldemailer.dto.response.CampaignResponse;
import io.github.nikilsaini.outreach.coldemailer.dto.response.FollowupResponse;
import io.github.nikilsaini.outreach.coldemailer.dto.response.GmailSendResponse;
import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import io.github.nikilsaini.outreach.coldemailer.entity.User;
import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;
import io.github.nikilsaini.outreach.coldemailer.exception.CampaignNotFoundException;
import io.github.nikilsaini.outreach.coldemailer.exception.IllegalCampaignStateException;
import io.github.nikilsaini.outreach.coldemailer.exception.UserNotFoundException;
import io.github.nikilsaini.outreach.coldemailer.repository.CampaignRepository;
import io.github.nikilsaini.outreach.auth.oauth.service.GoogleOAuthService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

  private final CampaignRepository campaignRepository;
  private final UserService userService;
  private final GoogleOAuthService googleOAuthService;
  private final GmailService gmailService;
  private final FollowupService followupService;

  @Transactional
  public CampaignResponse createWithFollowups(CreateCampaignRequest request) {
    log.atInfo().setMessage("Creating campaign")
        .addKeyValue("userId", request.userId())
        .addKeyValue("recipient", request.recipientEmail())
        .addKeyValue("followups", request.followupCount())
        .log();
    User user = userService.getById(request.userId());

    String refreshToken = userService.getDecryptedRefreshToken(request.userId());
    String accessToken = googleOAuthService.refreshAccessToken(refreshToken).accessToken();

    List<String> followupBodies = followupService.generateBodies(
        request.subject(), request.initialBody(), request.followupCount()
    );

    // A failure here aborts campaign creation with no retry; deferred "retry for failed
    // send" behaviour tracked as OPEN-DECISION-2 in docs/OPEN_DECISIONS.md.
    GmailSendResponse emailResponse = gmailService.sendEmail(
        accessToken, user.getEmail(), request.recipientEmail(), request.subject(), request.initialBody()
    );

    Campaign saved = campaignRepository.save(
        CampaignMapper.toEntity(request, user, emailResponse.threadId(), emailResponse.id())
    );

    List<FollowupResponse> followups = followupService.save(
        saved, followupBodies, request.gapDays(), request.preferredHour()
    );

    log.atInfo().setMessage("Campaign created")
        .addKeyValue("campaignId", saved.getId())
        .addKeyValue("threadId", saved.getGmailThreadId())
        .addKeyValue("followups", followups.size())
        .log();
    return FollowupMapper.toCampaignResponse(saved, followups);
  }

  @Transactional(readOnly = true)
  public List<CampaignResponse> listForUser(UUID userId) {
    if (!userService.existsById(userId)) {
      throw new UserNotFoundException(userId);
    }
    return campaignRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toResponseWithFollowups)
        .toList();
  }

  @Transactional(readOnly = true)
  public CampaignResponse getById(UUID campaignId) {
    Campaign campaign = campaignRepository.findById(campaignId)
        .orElseThrow(() -> new CampaignNotFoundException(campaignId));
    return toResponseWithFollowups(campaign);
  }

  @Transactional
  public CampaignResponse pause(UUID campaignId) {
    Campaign campaign = campaignRepository.findById(campaignId)
        .orElseThrow(() -> new CampaignNotFoundException(campaignId));
    if (campaign.getStatus() != CampaignStatus.ACTIVE) {
      throw new IllegalCampaignStateException(campaignId, campaign.getStatus(), "pause");
    }
    campaign.setStatus(CampaignStatus.PAUSED);
    log.atInfo().setMessage("Campaign paused").addKeyValue("campaignId", campaignId).log();
    return toResponseWithFollowups(campaign);
  }

  @Transactional
  public CampaignResponse resume(UUID campaignId) {
    Campaign campaign = campaignRepository.findById(campaignId)
        .orElseThrow(() -> new CampaignNotFoundException(campaignId));
    if (campaign.getStatus() != CampaignStatus.PAUSED) {
      throw new IllegalCampaignStateException(campaignId, campaign.getStatus(), "resume");
    }
    // Overdue follow-ups dispatch on the next scheduler tick; deferred "reschedule from
    // today" behaviour tracked as OPEN-DECISION-1 in docs/OPEN_DECISIONS.md.
    campaign.setStatus(CampaignStatus.ACTIVE);
    log.atInfo().setMessage("Campaign resumed").addKeyValue("campaignId", campaignId).log();
    return toResponseWithFollowups(campaign);
  }

  private CampaignResponse toResponseWithFollowups(Campaign campaign) {
    List<FollowupResponse> followups = followupService.getFollowUpsForCampaign(campaign.getId());
    return FollowupMapper.toCampaignResponse(campaign, followups);
  }
}

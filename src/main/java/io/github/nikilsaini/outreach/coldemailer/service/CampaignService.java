package io.github.nikilsaini.outreach.coldemailer.service;

import io.github.nikilsaini.outreach.coldemailer.dto.event.FollowupGenerationPayload;
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
import io.github.nikilsaini.outreach.coldemailer.exception.InvalidRecipientDomainException;
import io.github.nikilsaini.outreach.coldemailer.exception.UserNotFoundException;
import java.util.Hashtable;
import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import io.github.nikilsaini.outreach.coldemailer.repository.CampaignRepository;
import io.github.nikilsaini.outreach.auth.oauth.service.GoogleOAuthService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public CampaignResponse createWithFollowups(UUID userId, CreateCampaignRequest request) {
    log.atInfo().setMessage("Creating campaign")
        .addKeyValue("userId", userId)
        .addKeyValue("recipient", request.recipientEmail())
        .addKeyValue("followups", request.followupCount())
        .log();
    User user = userService.getById(userId);

    String refreshToken = userService.getDecryptedRefreshToken(userId);
    String accessToken = googleOAuthService.refreshAccessToken(refreshToken).accessToken();

    validateRecipientDomain(request.recipientEmail());

    GmailSendResponse emailResponse;
    try {
      emailResponse = gmailService.sendEmail(
          accessToken, user.getEmail(), request.recipientEmail(), request.subject(), request.initialBody()
      );
    } catch (Exception e) {
      log.atWarn().setMessage("Initial email delivery failed — saving campaign as FAILED")
          .addKeyValue("recipient", request.recipientEmail())
          .setCause(e).log();
      Campaign failed = CampaignMapper.toEntity(request, user, "", "");
      failed.setStatus(CampaignStatus.FAILED);
      Campaign saved = campaignRepository.save(failed);
      return FollowupMapper.toCampaignResponse(saved, List.of());
    }

    // Save campaign first so follow-up stubs can reference its ID.
    Campaign saved = campaignRepository.save(
        CampaignMapper.toEntity(request, user, emailResponse.threadId(), emailResponse.id())
    );

    // Persist GENERATING stubs — these are returned immediately to the caller so the UI can
    // show the timeline with a "generating" placeholder while Kafka handles Gemini in the background.
    List<FollowupResponse> stubs = followupService.saveStubs(
        saved, request.followupCount(), request.gapDays(), request.preferredHour()
    );

    // Publish the generation event AFTER commit via @TransactionalEventListener so we never
    // publish to Kafka for a transaction that rolled back.
    eventPublisher.publishEvent(new FollowupGenerationPayload(
        saved.getId(),
        request.subject(),
        request.initialBody(),
        request.followupCount(),
        request.gapDays(),
        request.preferredHour()
    ));

    log.atInfo().setMessage("Campaign created — followup generation dispatched")
        .addKeyValue("campaignId", saved.getId())
        .addKeyValue("threadId", saved.getGmailThreadId())
        .addKeyValue("stubs", stubs.size())
        .log();
    return FollowupMapper.toCampaignResponse(saved, stubs);
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

  @Transactional
  public CampaignResponse cancelFollowups(UUID campaignId) {
    Campaign campaign = campaignRepository.findById(campaignId)
        .orElseThrow(() -> new CampaignNotFoundException(campaignId));
    if (campaign.getStatus() != CampaignStatus.ACTIVE && campaign.getStatus() != CampaignStatus.PAUSED) {
      throw new IllegalCampaignStateException(campaignId, campaign.getStatus(), "cancel");
    }
    followupService.cancelPending(campaignId);
    campaign.setStatus(CampaignStatus.CANCELLED);
    campaignRepository.save(campaign);
    log.atInfo().setMessage("Campaign cancelled").addKeyValue("campaignId", campaignId).log();
    return toResponseWithFollowups(campaign);
  }

  private CampaignResponse toResponseWithFollowups(Campaign campaign) {
    List<FollowupResponse> followups = followupService.getFollowUpsForCampaign(campaign.getId());
    return FollowupMapper.toCampaignResponse(campaign, followups);
  }

  private void validateRecipientDomain(String email) {
    String domain = email.substring(email.indexOf('@') + 1);
    try {
      Hashtable<String, String> env = new Hashtable<>();
      env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
      InitialDirContext ctx = new InitialDirContext(env);
      Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
      if (attrs.get("MX") == null) {
        log.atWarn().setMessage("MX validation failed — no MX records").addKeyValue("domain", domain).log();
        throw new InvalidRecipientDomainException(domain);
      }
      log.atDebug().setMessage("MX validation passed").addKeyValue("domain", domain).log();
    } catch (InvalidRecipientDomainException e) {
      throw e;
    } catch (CommunicationException e) {
      // DNS server unreachable / timeout — fail open so transient network issues don't block valid sends
      log.atWarn().setMessage("MX lookup network error, proceeding with send").addKeyValue("domain", domain).log();
    } catch (NamingException e) {
      // Domain not found or any other DNS error — reject
      log.atWarn().setMessage("MX validation failed")
          .addKeyValue("domain", domain)
          .addKeyValue("reason", e.getClass().getSimpleName())
          .log();
      throw new InvalidRecipientDomainException(domain);
    }
  }
}

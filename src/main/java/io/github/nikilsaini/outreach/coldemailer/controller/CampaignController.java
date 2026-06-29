package io.github.nikilsaini.outreach.coldemailer.controller;

import io.github.nikilsaini.outreach.coldemailer.dto.request.CreateCampaignRequest;
import io.github.nikilsaini.outreach.coldemailer.dto.response.CampaignResponse;
import io.github.nikilsaini.outreach.coldemailer.service.CampaignService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {

  private final CampaignService campaignService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CampaignResponse createCampaign(
      @RequestBody CreateCampaignRequest request, Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    log.atDebug().setMessage("Request: create campaign").addKeyValue("userId", userId).log();
    return campaignService.createWithFollowups(userId, request);
  }

  @GetMapping
  public List<CampaignResponse> listCampaigns(Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    log.atDebug().setMessage("Request: list campaigns").addKeyValue("userId", userId).log();
    return campaignService.listForUser(userId);
  }

  @GetMapping("/{id}")
  public CampaignResponse getCampaign(@PathVariable("id") UUID id) {
    log.atDebug().setMessage("Request: get campaign").addKeyValue("campaignId", id).log();
    return campaignService.getById(id);
  }

  @PostMapping("/{id}/pause")
  public CampaignResponse pauseCampaign(@PathVariable("id") UUID id) {
    log.atDebug().setMessage("Request: pause campaign").addKeyValue("campaignId", id).log();
    return campaignService.pause(id);
  }

  @PostMapping("/{id}/resume")
  public CampaignResponse resumeCampaign(@PathVariable("id") UUID id) {
    log.atDebug().setMessage("Request: resume campaign").addKeyValue("campaignId", id).log();
    return campaignService.resume(id);
  }
}

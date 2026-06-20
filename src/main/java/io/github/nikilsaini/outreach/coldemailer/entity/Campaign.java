package io.github.nikilsaini.outreach.coldemailer.entity;

import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "campaigns")
@Data
public class Campaign extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "recipient_email", nullable = false)
  private String recipientEmail;

  @Column(name = "subject", nullable = false, columnDefinition = "TEXT")
  private String subject;

  @Column(name = "initial_body", nullable = false, columnDefinition = "TEXT")
  private String initialBody;

  @Column(name = "gmail_thread_id")
  private String gmailThreadId;

  @Column(name = "root_message_id")
  private String rootMessageId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CampaignStatus status;
}

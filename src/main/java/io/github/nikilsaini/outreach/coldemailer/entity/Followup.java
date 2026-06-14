package io.github.nikilsaini.outreach.coldemailer.entity;

import io.github.nikilsaini.outreach.coldemailer.enums.FollowupStatus;
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
@Table(name = "followups")
@Data
public class Followup extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "campaign_id", nullable = false)
  private Campaign campaign;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private FollowupStatus status;

  @Column(name = "sequence_number", nullable = false)
  private Integer sequenceNumber;

  @Column(name = "body", nullable = false, columnDefinition = "TEXT")
  private String body;
}

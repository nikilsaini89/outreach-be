package io.github.nikilsaini.outreach.coldemailer.repository;

import io.github.nikilsaini.outreach.coldemailer.entity.Followup;
import io.github.nikilsaini.outreach.coldemailer.enums.CampaignStatus;
import io.github.nikilsaini.outreach.coldemailer.enums.FollowupStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowupRepository extends JpaRepository<Followup, UUID> {

  @Query("SELECT f FROM Followup f JOIN FETCH f.campaign c JOIN FETCH c.user "
      + "WHERE f.status = :status AND f.scheduledAt <= :now AND c.status = :campaignStatus")
  List<Followup> findDueFollowups(
      @Param("status") FollowupStatus status,
      @Param("now") LocalDateTime now,
      @Param("campaignStatus") CampaignStatus campaignStatus);

  List<Followup> findByCampaignIdOrderBySequenceNumberAsc(UUID campaignId);

  long countByCampaignIdAndStatusIn(UUID campaignId, List<FollowupStatus> statuses);
}

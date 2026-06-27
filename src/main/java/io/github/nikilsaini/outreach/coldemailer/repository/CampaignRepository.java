package io.github.nikilsaini.outreach.coldemailer.repository;

import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

  List<Campaign> findByUserIdOrderByCreatedAtDesc(UUID userId);
}

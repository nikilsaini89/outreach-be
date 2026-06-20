package io.github.nikilsaini.outreach.coldemailer.repository;

import io.github.nikilsaini.outreach.coldemailer.entity.Campaign;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {}

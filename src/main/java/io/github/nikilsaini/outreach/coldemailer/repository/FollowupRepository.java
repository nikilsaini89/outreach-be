package io.github.nikilsaini.outreach.coldemailer.repository;

import io.github.nikilsaini.outreach.coldemailer.entity.Followup;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowupRepository extends JpaRepository<Followup, UUID> {}

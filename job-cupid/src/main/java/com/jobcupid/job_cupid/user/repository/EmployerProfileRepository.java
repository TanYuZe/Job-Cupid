package com.jobcupid.job_cupid.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobcupid.job_cupid.user.entity.EmployerProfile;

@Repository
public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, UUID> {

    Optional<EmployerProfile> findByUserId(UUID userId);
}

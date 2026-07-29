package com.familytree.repository;

import com.familytree.entity.AdminAccessRequest;
import com.familytree.entity.AdminAccessRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminAccessRequestRepository extends JpaRepository<AdminAccessRequest, Long> {
    List<AdminAccessRequest> findAllByStatusOrderByRequestedAtAsc(AdminAccessRequestStatus status);
    List<AdminAccessRequest> findByUserAccountId(Long userAccountId);
    Optional<AdminAccessRequest> findFirstByUserAccountIdAndStatusOrderByRequestedAtDesc(
            Long userAccountId, AdminAccessRequestStatus status);
}

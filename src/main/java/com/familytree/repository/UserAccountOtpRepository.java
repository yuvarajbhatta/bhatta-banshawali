package com.familytree.repository;

import com.familytree.entity.OtpPurpose;
import com.familytree.entity.UserAccountOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountOtpRepository extends JpaRepository<UserAccountOtp, Long> {

    List<UserAccountOtp> findByUserAccountIdAndPurposeAndConsumedAtIsNull(Long userAccountId, OtpPurpose purpose);

    Optional<UserAccountOtp> findFirstByUserAccountIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            Long userAccountId, OtpPurpose purpose);

    List<UserAccountOtp> findByUserAccountId(Long userAccountId);
}

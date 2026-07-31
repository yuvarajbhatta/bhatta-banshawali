package com.familytree.repository;

import com.familytree.entity.TokenPurpose;
import com.familytree.entity.UserAccountToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountTokenRepository extends JpaRepository<UserAccountToken, Long> {

    Optional<UserAccountToken> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);

    List<UserAccountToken> findByUserAccountIdAndPurposeAndConsumedAtIsNull(Long userAccountId, TokenPurpose purpose);
}

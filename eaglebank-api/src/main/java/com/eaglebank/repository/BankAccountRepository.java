package com.eaglebank.repository;

import com.eaglebank.entity.BankAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccountEntity, String> {
    Optional<BankAccountEntity> findByAccountNumber(String accountNumber);
   /* @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BankAccountEntity b WHERE b.accountNumber = :accountNumber")
    Optional<BankAccountEntity> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);*/

}

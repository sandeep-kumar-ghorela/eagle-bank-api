package com.eaglebank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "transactions")
public class TransactionEntity {
    public TransactionEntity() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "accountNumber", referencedColumnName = "accountNumber", nullable = false)
    private BankAccountEntity bankAccount;

    @Column(nullable = false, unique = true)
    private String transactionId;

    private String type;

    private BigDecimal amount;

    private String currency;

    private String createdTimestamp;

}

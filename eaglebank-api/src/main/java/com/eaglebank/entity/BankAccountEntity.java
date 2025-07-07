package com.eaglebank.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Entity
@Table(name = "accounts")
public class BankAccountEntity {

    public BankAccountEntity() {

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "userId", referencedColumnName = "userId") // Foreign key to User table
    private UserEntity user;


    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<TransactionEntity> transactions = new ArrayList<>();

    @Column(unique = true)
    private String accountNumber;

    private String sortCode;


    private String name;

    private String accountType;

    private BigDecimal balance;

    private String currency;

    private String createdTimestamp;

    private String updatedTimestamp;


}

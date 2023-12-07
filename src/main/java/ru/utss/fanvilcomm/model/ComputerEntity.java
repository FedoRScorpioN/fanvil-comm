package ru.utss.fanvilcomm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "computers")
@Data
public class ComputerEntity {

    @Id
    @Column(name = "computer_name")
    private String computerName;

    @Column(name = "mac_address")
    private String macAddress;
}
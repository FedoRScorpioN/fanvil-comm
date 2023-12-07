package ru.utss.fanvilcomm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "mac_ip_pairs")
@Data
public class MacIpPairEntity {

    @Id
    @Column(name = "mac_address")
    private String macAddress;

    @Column(name = "ip_address")
    private String ipAddress;
}
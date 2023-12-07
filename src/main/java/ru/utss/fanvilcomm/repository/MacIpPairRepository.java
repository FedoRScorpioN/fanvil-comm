package ru.utss.fanvilcomm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.utss.fanvilcomm.model.MacIpPairEntity;

import java.util.Optional;

@Repository
public interface MacIpPairRepository extends JpaRepository<MacIpPairEntity, String> {
    Optional<MacIpPairEntity> findById(String macAddress);
}

package ru.utss.fanvilcomm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.utss.fanvilcomm.model.ComputerEntity;

import java.util.Optional;

@Repository
public interface ComputerRepository extends JpaRepository<ComputerEntity, String> {
    Optional<ComputerEntity> findByComputerName(String computerName);
}
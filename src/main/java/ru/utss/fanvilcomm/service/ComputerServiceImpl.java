package ru.utss.fanvilcomm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.utss.fanvilcomm.model.ComputerEntity;
import ru.utss.fanvilcomm.repository.ComputerRepository;

import java.util.Optional;

@Service
public class ComputerServiceImpl implements ComputerService {

    @Autowired
    private ComputerRepository computerRepository;

    @Override
    public String getMacAddressByComputerName(String computerName) {
        Optional<ComputerEntity> computerEntityOptional = computerRepository.findByComputerName(computerName);
        return computerEntityOptional.map(ComputerEntity::getMacAddress).orElse(null);
    }
}
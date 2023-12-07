package ru.utss.fanvilcomm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.utss.fanvilcomm.model.MacIpPairEntity;
import ru.utss.fanvilcomm.repository.MacIpPairRepository;

import java.util.Optional;

@Service
public class MacIpPairServiceImpl implements MacIpPairService {

    @Autowired
    private MacIpPairRepository macIpPairRepository;

    @Override
    public String getIpAddressByMacAddress(String macAddress) {
        Optional<MacIpPairEntity> macIpPairEntityOptional = macIpPairRepository.findById(macAddress);
        return macIpPairEntityOptional.map(MacIpPairEntity::getIpAddress).orElse(null);
    }
}
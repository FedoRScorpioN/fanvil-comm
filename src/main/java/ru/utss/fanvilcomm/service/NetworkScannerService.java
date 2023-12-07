package ru.utss.fanvilcomm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.utss.fanvilcomm.model.MacIpPairEntity;
import ru.utss.fanvilcomm.repository.MacIpPairRepository;

import java.io.*;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NetworkScannerService {

    @Autowired
    private MacIpPairRepository macIpPairRepository;

    private static final Logger logger = LoggerFactory.getLogger(NetworkScannerService.class);

    @Value("${nmapPath}")
    private String nmapPath;
    @Value("${subnet1}")
    private String subnet1;
    @Value("${subnet2}")
    private String subnet2;
    @Value("${nmap.output.directory}")
    private String nmapOutputDirectory;
    @Value("${nmapOutFile1}")
    private String nmapOutFile1;
    @Value("${nmapOutFile2}")
    private String nmapOutFile2;
    @Value("${macIpPairs}")
    private String macIpPairs;

    @Scheduled(initialDelay = 0, fixedDelayString = "${nmap.scan.interval}")
    public void scanAndSaveToDatabase() {
        try {
            if (!isDatabaseEmpty()) {
                clearDatabase();
            }

            executeNmapAndSaveToFile(subnet1, nmapOutFile1);
            executeNmapAndSaveToFile(subnet2, nmapOutFile2);

            clearDatabase();

            reformatAndSaveMacIpPairs(nmapOutFile1, macIpPairs);
            reformatAndSaveMacIpPairs(nmapOutFile2, macIpPairs);

            saveToDatabaseFromFormattedFile(macIpPairs);

            deleteFile(nmapOutFile1);
            deleteFile(nmapOutFile2);
            deleteFile(macIpPairs);

        } catch (IOException | InterruptedException e) {
            logger.error("Произошла ошибка во время сканирования сети и сохранения в базу данных", e);
        }
    }

    private boolean isDatabaseEmpty() {
        return macIpPairRepository.count() == 0;
    }

    private void executeNmapAndSaveToFile(String subnet, String fileName) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(nmapPath + " -sn " + subnet);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             FileWriter writer = new FileWriter(Paths.get(nmapOutputDirectory, fileName).toFile())) {

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                writer.write(line + "\n");
            }

            Thread.sleep(30000);

            process.waitFor();
        }
    }

    private void reformatAndSaveMacIpPairs(String inputFileName, String outputFileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(nmapOutputDirectory, inputFileName)));
             FileWriter writer = new FileWriter(new File(nmapOutputDirectory, outputFileName), true)) {

            String line;
            String currentIp = null;
            String currentMac;
            boolean skipFirstLine = true;

            Pattern ipPattern = Pattern.compile("Nmap scan report for (10\\.10\\.\\d{1,3}\\.\\d{1,3})");
            Pattern macPattern = Pattern.compile("MAC Address: ([0-9A-Fa-f:]+)");

            while ((line = reader.readLine()) != null) {
                if (skipFirstLine) {
                    skipFirstLine = false;
                    continue;
                }

                Matcher ipMatcher = ipPattern.matcher(line);
                Matcher macMatcher = macPattern.matcher(line);

                if (ipMatcher.find()) {
                    currentIp = ipMatcher.group(1);
                }

                if (macMatcher.find() && currentIp != null) {
                    currentMac = macMatcher.group(1);
                    String result = currentMac + "==" + currentIp + "\n";
                    writer.write(result);

                    currentIp = null;
                    currentMac = null;
                }
            }
        }
    }

    private void saveToDatabaseFromFormattedFile(String inputFileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(nmapOutputDirectory, inputFileName)))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("==");
                if (parts.length == 2) {
                    String macAddress = parts[0];
                    String ipAddress = parts[1];
                    saveToDatabase(macAddress, ipAddress);
                }
            }
        }
    }

    private void saveToDatabase(String macAddress, String ipAddress) {
        MacIpPairEntity macIpPairEntity = new MacIpPairEntity();
        macIpPairEntity.setMacAddress(macAddress);
        macIpPairEntity.setIpAddress(ipAddress);

        try {
            macIpPairRepository.save(macIpPairEntity);
            logger.info("Сохранено в базе данных: MAC = {}, IP = {}", macAddress, ipAddress);
        } catch (DataAccessException e) {
            logger.error("Произошла ошибка при сохранении в базу данных", e);
        }
    }

    private void clearDatabase() {
        try {
            macIpPairRepository.deleteAll();
            logger.info("База данных очищена перед сканированием");
        } catch (Exception e) {
            logger.error("Произошла ошибка при очистке базы данных", e);
        }
    }

    private void deleteFile(String fileName) {
        File fileToDelete = new File(Paths.get(nmapOutputDirectory, fileName).toString());
        if (fileToDelete.exists() && fileToDelete.isFile()) {
            if (fileToDelete.delete()) {
                logger.info("Файл удален: {}", fileName);
            } else {
                logger.warn("Невозможно удалить файл: {}", fileName);
            }
        }
    }

}
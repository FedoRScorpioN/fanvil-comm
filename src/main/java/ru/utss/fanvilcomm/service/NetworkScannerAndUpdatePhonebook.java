package ru.utss.fanvilcomm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class NetworkScannerAndUpdatePhonebook {

    private static final Logger logger = LoggerFactory.getLogger(NetworkScannerAndUpdatePhonebook.class);

    @Scheduled(initialDelay = 0, fixedDelayString = "${nmap.scan.interval}")
    private void runPythonScript() {
        try {
            String pythonCommand = "C:\\Users\\User\\AppData\\Local\\Programs\\Python\\Python311\\python.exe";
            String pythonScriptPath = "src/main/python/Main.py";

            ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, pythonScriptPath);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Python-скрипт успешно выполнен.");
            } else {
                logger.error("Ошибка при выполнении Python-скрипта. Код возврата: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Ошибка при выполнении Python-скрипта", e);
        }
    }

}
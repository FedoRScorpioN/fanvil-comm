package ru.utss.fanvilcomm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class NetworkScannerAndUpdatePhonebook {

    private static final Logger logger = LoggerFactory.getLogger(NetworkScannerAndUpdatePhonebook.class);
    @Value("${python.pythonCommand}")
    private String pythonCommand;
    @Value("${python.pythonScriptPath}")
    private String pythonScriptPath;


    @Scheduled(initialDelay = 0, fixedDelayString = "${nmap.scan.interval}")
    private void runPythonScript() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, "-Xutf8", pythonScriptPath);
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT); // Добавлено
            Process process = processBuilder.start();

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
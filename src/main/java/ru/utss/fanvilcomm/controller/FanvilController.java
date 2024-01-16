package ru.utss.fanvilcomm.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.utss.fanvilcomm.service.ComputerService;
import ru.utss.fanvilcomm.service.MacIpPairService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/fanvil")
public class FanvilController {
    private static final Logger logger = LoggerFactory.getLogger(FanvilController.class);
    @Autowired
    private final ComputerService computerService;
    @Autowired
    private final MacIpPairService macIpPairService;

    @Value("${phone.port}")
    private int phonePort;
    @Value("${phone.username}")
    private String phoneUsername;
    @Value("${phone.password}")
    private String phonePassword;

    public FanvilController(ComputerService computerService, MacIpPairService macIpPairService) {
        this.computerService = computerService;
        this.macIpPairService = macIpPairService;
    }

    @RequestMapping("/index")
    public String index(
            HttpServletRequest request,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "peerGUID", required = false) String peerGUID
    ) {
        try {
            if (peerGUID == null || peerGUID.isEmpty()) {
                peerGUID = request.getRemoteAddr();
            }

            String computerName = getComputerNameFromIp(peerGUID);

            if (phoneNumber != null) {
                logger.info("GET-запрос от пользователя с GUID-адресом '{}' и именем ПК '{}' и номером телефона '{}'",
                        peerGUID, computerName, phoneNumber);
            } else {
                logger.info("GET-запрос от пользователя с GUID-адресом '{}' и именем ПК '{}'", peerGUID, computerName);
            }

            String macAddress = computerService.getMacAddressByComputerName(computerName);

            if (macAddress != null) {
                String phoneIpAddress = macIpPairService.getIpAddressByMacAddress(macAddress);

                if (phoneIpAddress != null) {
                    logger.info("IP-адрес телефона для GUID '{}' и именем ПК '{}': '{}'",
                            peerGUID, computerName, phoneIpAddress);
                    initiateCall(phoneIpAddress, phonePort, phoneUsername, phonePassword, phoneNumber);

                } else {
                    logger.error("IP-адрес телефона для GUID '{}' и именем ПК '{}' не найден", peerGUID, computerName);
                }

            } else {
                logger.error("MAC-адрес для GUID '{}' и именем ПК '{}' не найден", peerGUID, computerName);
            }

        } catch (Exception e) {
            logger.error("Произошла ошибка при обработке GET-запроса", e);
        }
        return phoneNumber;
    }

    private String getComputerNameFromIp(String ipAddress) {
        String result = runTracertCommand(ipAddress);
        return parseComputerNameFromTracertResult(result);
    }

    private String runTracertCommand(String peerGUID) {
        StringBuilder result = new StringBuilder();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "chcp 65001 > nul & tracert ",
                    peerGUID);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            boolean exitCode = process.waitFor(1, TimeUnit.SECONDS);
            if (!exitCode) {
                logger.error("Ошибка выполнения команды tracert. Код завершения: {}", process.exitValue());
            }

            logger.info("Полный вывод команды tracert: '{}'", result);

        } catch (IOException | InterruptedException e) {
            logger.error("Ошибка при выполнении команды tracert", e);
        }

        return result.toString();
    }

    private String parseComputerNameFromTracertResult(String tracertResult) {
        int endIndex = tracertResult.indexOf(".TUTSS.local");
        if (endIndex != -1) {
            int startIndex = tracertResult.lastIndexOf(' ', endIndex);
            if (startIndex != -1) {
                return tracertResult.substring(startIndex + 1, endIndex);
            } else {
                return tracertResult.substring(0, endIndex);
            }
        } else {
            logger.error("Не удалось извлечь имя ПК из вывода команды tracert");
            return "Не удалось определить имя ПК";
        }
    }

    private static void initiateCall(String phoneIpAddress, int phonePort, String username, String password,
                                     String phoneNumber) {

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {

            String url = String.format("http://%s:%d/cgi-bin/ConfigManApp.com?key=SPEAKER;%s;ENTER",
                    phoneIpAddress, phonePort, phoneNumber);

            HttpGet request = new HttpGet(url);

            try {
                CloseableHttpResponse response = httpClient.execute(request);

                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
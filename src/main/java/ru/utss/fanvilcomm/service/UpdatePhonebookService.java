package ru.utss.fanvilcomm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UpdatePhonebookService {

    private static final Logger logger = LoggerFactory.getLogger(UpdatePhonebookService.class);

    @Value("${excel.file.path}")
    private String excelFilePath;

    @Value("${json.file.path}")
    private String jsonFilePath;

    @Value("${html.file.path}")
    private String htmlFilePath;

    @Scheduled(initialDelay = 0, fixedRate = 600000)
    public void runUpdateJob() {
        try {
            logger.info("Начало обновления справочника...");
            convertExcelToJson();
            logger.info("Справочник успешно обновлен.");
        } catch (IOException e) {
            logger.error("Произошла ошибка при обновлении справочника", e);
        }
    }

    public void convertExcelToJson() throws IOException {
        try (FileInputStream excelFile = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(excelFile)) {

            ObjectMapper mapper = new ObjectMapper();
            ArrayNode jsonArray = mapper.createArrayNode();
            boolean skipRows = true;

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);

                for (Row row : sheet) {
                    if (skipRows && sheetIndex == 0) {
                        if (row.getRowNum() < 3) {
                            continue;
                        } else {
                            skipRows = false;
                        }
                    }

                    ObjectNode jsonRow = mapper.createObjectNode();

                    for (int i = 0; i < 8; i++) {
                        Cell cell = row.getCell(i);
                        String columnName = getColumnName(i);

                        if (cell == null || cell.getCellType() == CellType.BLANK) {
                            jsonRow.putNull(columnName);
                        } else if (cell.getCellType() == CellType.STRING) {
                            jsonRow.put(columnName, cell.getStringCellValue());
                        } else if (cell.getCellType() == CellType.NUMERIC) {
                            jsonRow.put(columnName, formatNumericCellValue(cell));
                        }
                    }

                    System.out.println("Обработана строка " + row.getRowNum() + ": " + jsonRow);

                    jsonArray.add(jsonRow);
                }
            }

            Path jsonPath = Path.of(jsonFilePath);
            if (Files.exists(jsonPath)) {
                Files.delete(jsonPath);
                logger.info("Удален существующий JSON-файл: {}", jsonFilePath);
            }

            Files.createFile(jsonPath);
            logger.info("Создан новый JSON-файл: {}", jsonFilePath);

            try (FileOutputStream jsonFile = new FileOutputStream(jsonFilePath)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, jsonArray);
            }
            updateHtmlWithJson(jsonFilePath, htmlFilePath);

            logger.info("Конвертация успешно завершена.");

        } catch (IOException e) {
            logger.error("Произошла ошибка во время конвертации.", e);
        }
    }

    private static String formatNumericCellValue(Cell cell) {
        double value = cell.getNumericCellValue();
        long longValue = (long) value;
        if (value == longValue) {
            return String.valueOf(longValue);
        } else {
            DecimalFormat decimalFormat = new DecimalFormat("#");
            return decimalFormat.format(value);
        }
    }

    private static String getColumnName(int index) {
        return switch (index) {
            case 0 -> "Должность";
            case 1 -> "ФИО";
            case 2 -> "Рабочий";
            case 3 -> "Внутренний";
            case 4 -> "Сотовый";
            case 5 -> "Местоположение";
            case 6 -> "Email";
            case 7 -> "Дата рождения";
            default -> "";
        };
    }

    private static void updateHtmlWithJson(String jsonFilePath, String htmlFilePath) throws IOException {
        String jsonData = Files.readString(Path.of(jsonFilePath));
        String htmlContent = Files.readString(Path.of(htmlFilePath));

        String pattern = "var jsonData = .*?\\];";
        Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = regex.matcher(htmlContent);

        if (matcher.find()) {
            htmlContent = htmlContent.replace(matcher.group(), "var jsonData = " + jsonData + ";");
        } else {
            htmlContent = htmlContent.replaceFirst("</script>", "var jsonData = " + jsonData + ";</script>");
        }

        Files.writeString(Path.of(htmlFilePath), htmlContent);
    }
}

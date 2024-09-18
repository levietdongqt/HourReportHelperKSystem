package com.mytech.hourreporthelper;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@Controller
public class HomeController {
    @GetMapping("/")
    public String showForm(Model model,@RequestParam(value = "success", required = false) String success) {
        boolean isShowSuccess = success != null;
        model.addAttribute("isShowSuccess", isShowSuccess);
        model.addAttribute("form", new InputForm());
        return "index";
    }

    @PostMapping("/submit")
    public String submitForm(@ModelAttribute InputForm form) throws IOException {

        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate now = LocalDate.now();
        String currentDate = now.format(formatter2);

        HashMap<Integer, String> reportTime = new HashMap<>();
        reportTime.put(0, "09");
        reportTime.put(1, "10");
        reportTime.put(2, "11");
        reportTime.put(3, "12");
        reportTime.put(4, "14");
        reportTime.put(5, "15");
        reportTime.put(6, "16");
        reportTime.put(7, "17");

        HashMap<Integer, String> startTime = new HashMap<>();
        startTime.put(0, "08:15");
        startTime.put(1, "09:00");
        startTime.put(2, "10:00");
        startTime.put(3, "11:00");
        startTime.put(4, "13:00");
        startTime.put(5, "14:00");
        startTime.put(6, "15:00");
        startTime.put(7, "16:00");
        String fileNameTemp = "Hour Report_DevHCM_" + form.getEmployeeName() + "_" + currentDate + "_";
        File parentDir = null;
        for (int i = 0; i < 8; i++) {
            if (form.getDescriptions().get(i).isEmpty()) continue;

            String fileName = fileNameTemp + reportTime.get(i) + "H" + ".xlsx";
            String location = "D:/HourReport/" + currentDate + "/" + fileName;
            File file = new File(location);
            parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.getParentFile().exists() && parentDir.getParentFile() != null) {
                    parentDir.getParentFile().mkdirs();
                }
                parentDir.mkdirs();
            }
            try (XSSFWorkbook workbook = new XSSFWorkbook();) {
                Sheet sheet = workbook.createSheet("Sheet 1");
                Row headerRow = sheet.createRow(0);
                CellStyle cellStyle = workbook.createCellStyle();
                // Căn giữa theo chiều ngang và chiều dọc
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                cellStyle.setWrapText(true);
                headerRow.createCell(0).setCellValue("Report Date");
                headerRow.createCell(1).setCellValue("Report time");
                headerRow.createCell(2).setCellValue("Starting time");
                Cell cell = headerRow.createCell(3);
                cell.setCellValue("Job Content");
                cell.setCellStyle(cellStyle);
                Cell cellComplate = headerRow.createCell(4);
                cellComplate.setCellStyle(cellStyle);
                cellComplate.setCellValue("Completed Y/N");
                headerRow.createCell(5).setCellValue("Completing time");
                headerRow.createCell(6).setCellValue("Remark");

                sheet.setColumnWidth(1, 3200); // Độ rộng của cột B

                sheet.setColumnWidth(3, 32000); // Độ rộng của cột B
                sheet.setColumnWidth(0, 3200); // Độ rộng của cột B
                sheet.setColumnWidth(2, 3200); // Độ rộng của cột B
                sheet.setColumnWidth(1, 3200); // Độ rộng của cột B
                sheet.setColumnWidth(4, 4000); // Độ rộng của cột B
                sheet.setColumnWidth(5, 4000); // Độ rộng của cột B

                for (int rowNum = 0; rowNum <= 7; rowNum++) {
                    if (rowNum > i) break;
                    boolean isCompleted = form.getIsCompletes().get(rowNum) != null;
                    Row row = sheet.createRow(rowNum + 1);

                    String description = form.getDescriptions().get(rowNum);
                    int numberOfLines  = description.split("\n").length;
                    row.setHeightInPoints(numberOfLines * sheet.getDefaultRowHeightInPoints());
                    row.createCell(0).setCellValue(LocalDate.now().format(formatter1));
                    row.createCell(1).setCellValue(reportTime.get(rowNum));
                    row.createCell(2).setCellValue(startTime.get(rowNum));

                    Cell jobContentCell = row.createCell(3);

                    CellStyle cellStyle2 = workbook.createCellStyle();
                    cellStyle2.setAlignment(HorizontalAlignment.LEFT);
                    cellStyle2.setWrapText(true);
                    jobContentCell.setCellStyle(cellStyle2);
                    jobContentCell.setCellValue(description);

                    Cell cellComplete = row.createCell(4);
                    cellComplete.setCellStyle(cellStyle2);
                    cellComplete.setCellValue(isCompleted ? "Y" : "N");

                    Cell cell1 = row.createCell(5);
                    cell1.setCellStyle(cellStyle2);
                    cell1.setCellValue(form.completingTimes.get(rowNum).isEmpty() ? "" : form.completingTimes.get(rowNum));

                    Cell cell2 = row.createCell(6);
                    cell2.setCellStyle(cellStyle2);
                    cell2.setCellValue(form.getRemarks().get(rowNum));
                }

                FileOutputStream fos = new FileOutputStream(file);
                workbook.write(fos);
            }
        }
        return "redirect:/?success=true";
    }
}

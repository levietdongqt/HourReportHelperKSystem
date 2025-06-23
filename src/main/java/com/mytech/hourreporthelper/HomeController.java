package com.mytech.hourreporthelper;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class HomeController {
    @GetMapping("/old")
    public String showForm(Model model,
                           @RequestParam(value = "success", required = false) String success,
                           @RequestParam(value = "currentDate", required = false) String currentDate,
                           @RequestParam(value = "employeeName", required = false) String employeeName) {
        boolean isShowSuccess = success != null;
        model.addAttribute("isShowSuccess", isShowSuccess);
        model.addAttribute("currentDate", currentDate);
        model.addAttribute("employeeName", employeeName);
        model.addAttribute("now", LocalDate.now());
        model.addAttribute("form", new InputForm());
        return "index";
    }

    @GetMapping("/old/admin")
    public String admin(Model model) {
        model.addAttribute("login", new login());
        return "admin";
    }


    @PostMapping("/old/login")
    public ResponseEntity<Resource> login(@ModelAttribute login form) throws IOException {
        // Kiểm tra username và password
        if (!form.getPassword().equals("donglv1!00") || !form.getUsername().equals("donglv")) {
            return ResponseEntity.badRequest().body(null);
        }

        // Định nghĩa thư mục nguồn và tệp zip tạm
        Path sourceDir = Paths.get("/tmp");
        Path zipFile = Paths.get("/tmp/result.zip"); // Tệp zip sẽ được lưu tạm ở thư mục /tmp

        // Kiểm tra sự tồn tại của thư mục nguồn
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        // Nén thư mục
        FileUtil.zipDirectory(sourceDir, zipFile);

        // Chuyển tệp zip thành Resource để gửi cho người dùng
        Resource resource = new FileSystemResource(zipFile);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Sử dụng ScheduledExecutorService để xóa tệp zip sau 5 giây
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                // Xóa tệp zip
                Files.deleteIfExists(zipFile);
            } catch (IOException e) {
                e.printStackTrace(); // Log lỗi nếu không xóa được
            }
        }, 10, TimeUnit.SECONDS);

        // Trả về tệp zip cho người dùng
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFile.getFileName() + "\"")
                .body(resource);
    }


    @PostMapping("/old/submit")
    public String submitForm(@ModelAttribute InputForm form) throws IOException, InterruptedException {
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate now = form.getReportDate();
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
        String fileNameTemp = "Hour Report_" + form.getDept() + "_" + form.getEmployeeName() + "_" + currentDate + "_";
        File parentDir = null;

        for (int i = 0; i < 8; i++) {
            if (form.getDescriptions().get(i).isEmpty()) continue;

            String fileName = fileNameTemp + reportTime.get(i) + "H" + ".xlsx";
            String location = "/tmp/" + form.getEmployeeName() + "/HourReport/" + currentDate + "/" + fileName;
            File file = new File(location);
            parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.getParentFile().exists() && parentDir.getParentFile() != null) {
                    parentDir.getParentFile().mkdirs();
                }
                parentDir.mkdirs();
            }
            try (XSSFWorkbook workbook = new XSSFWorkbook();

                 FileOutputStream fos = new FileOutputStream(file)) {
                Sheet sheet = workbook.createSheet("Sheet 1");


                Row headerRow = sheet.createRow(0);

                CellStyle cellStyle = getCellStyleForHeader(workbook);

                Cell cell0 = handleCellString(headerRow, 0, cellStyle, "ReportDate");
                Cell cell1 = handleCellString(headerRow, 1, cellStyle, "Report time");
                Cell cell2 = handleCellString(headerRow, 2, cellStyle, "Starting time");
                Cell cell3 = handleCellString(headerRow, 3, cellStyle, "Job Content");
                Cell cell4 = handleCellString(headerRow, 4, cellStyle, "Completed Y/N");
                Cell cell5 = handleCellString(headerRow, 5, cellStyle, "Completing time");
                Cell cell6 = handleCellString(headerRow, 6, cellStyle, "Remark");

                sheet.setColumnWidth(1, 3200);
                sheet.setColumnWidth(3, 26000);
                sheet.setColumnWidth(0, 3200);
                sheet.setColumnWidth(2, 3100);
                sheet.setColumnWidth(1, 3200);
                sheet.setColumnWidth(4, 4000);
                sheet.setColumnWidth(5, 4000);
                sheet.setColumnWidth(6, 15000);
                for (int rowNum = 0; rowNum <= 7; rowNum++) {
                    if (rowNum > i) break;
                    Row row = sheet.createRow(rowNum + 1);
                    row.setHeight((short) -1);
                    String description = form.getDescriptions().get(rowNum);
                    CellStyle cellStyle2 = getCelStyleForData(workbook);

                    Cell cellR0 = row.createCell(0);
                    cellR0.setCellStyle(cellStyle);
                    cellR0.setCellValue(form.getReportDate().format(formatter1));

                    Cell cellR1 = row.createCell(1);
                    cellR1.setCellStyle(cellStyle);
                    cellR1.setCellValue(reportTime.get(rowNum));

                    Cell cellR2 = row.createCell(2);
                    cellR2.setCellStyle(cellStyle);
                    cellR2.setCellValue(form.startTimes.get(rowNum).isEmpty() ? "" : form.startTimes.get(rowNum));

                    Cell jobContentCell = row.createCell(3);
                    jobContentCell.setCellStyle(cellStyle2);
                    jobContentCell.setCellValue(description);

                    Cell cellComplete = row.createCell(4);
                    cellComplete.setCellStyle(cellStyle);
                    cellComplete.setCellValue(form.getIsCompletes().get(rowNum));

                    Cell cellR5 = row.createCell(5);
                    cellR5.setCellStyle(cellStyle2);
                    cellR5.setCellValue(form.completingTimes.get(rowNum).isEmpty() ? "" : form.completingTimes.get(rowNum));

                    Cell cellR6 = row.createCell(6);
                    cellR6.setCellStyle(cellStyle2);
                    cellR6.setCellValue(form.getRemarks().get(rowNum));
                }
                workbook.write(fos);
            }
        }

        Path sourceDir = Paths.get("/tmp/" + form.getEmployeeName());
        Path filePath = sourceDir.resolve("hello.txt");
        String content = currentDate + System.lineSeparator(); // Thêm một dòng mới

        try {
            // Tạo thư mục nếu chưa tồn tại
            Files.createDirectories(sourceDir);
            // Ghi nội dung vào tệp hello.txt với tùy chọn append
            Files.write(filePath, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("File đã được ghi thành công tại: " + filePath);
        } catch (IOException e) {
            System.out.println("Có lỗi xảy ra: " + e.getMessage());
        }


        return "redirect:/?success=true&currentDate=" + currentDate + "&employeeName=" + form.getEmployeeName();

    }

    @GetMapping("/downloadAll/{currentDate}")
    public ResponseEntity<Resource> downloadAllFiles(@PathVariable String currentDate, @RequestParam String employeeName) throws IOException {
        // Thư mục chứa các file cần tải

        Path sourceDir = Paths.get("/tmp/" + employeeName + "/HourReport/" + currentDate);

        // Tạo tệp zip tạm để lưu các file
        Path zipFile = Paths.get("/tmp/" + employeeName + "/" + currentDate + ".zip");

        // Tạo tệp zip từ thư mục
        FileUtil.zipDirectory(sourceDir, zipFile);
        // Chuyển tệp zip thành Resource để gửi cho người dùng
        Resource resource = new FileSystemResource(zipFile);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        new Thread(() -> {
            try {
                // Thêm khoảng thời gian chờ (ví dụ 10 giây)
                Thread.sleep(5000);

                // Xóa tệp zip
                Files.deleteIfExists(zipFile);

                // Xóa thư mục và tất cả file bên trong
                deleteDirectoryRecursively(sourceDir.getParent());  // Xóa cả thư mục /HourReport/ của employeeName
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(); // Log lỗi nếu không xóa được
            }
        }).start();
        // Xóa tệp zip sau khi phục vụ người dùng (nếu cần)
        // Files.delete(zipFile);
        //deleteDirectoryRecursively(sourceDir.getParent());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFile.getFileName() + "\"")
                .body(resource);
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            // Sử dụng Files.walk để xử lý đệ quy an toàn
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder()) // Đảm bảo xóa từ trong ra ngoài
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    private static Cell handleCellString(Row headerRow, int col, CellStyle cellStyle, String value) {
        Cell cell0 = headerRow.createCell(col);
        cell0.setCellStyle(cellStyle);
        cell0.setCellValue(value);
        return cell0;
    }

    private static CellStyle getCellStyleForHeader(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setFontName("Arial"); // Thiết lập font là Times New Roman
        font.setFontHeightInPoints((short) 11);
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);
        cellStyle.setBorderTop(BorderStyle.THIN);      // Viền trên
        cellStyle.setBorderBottom(BorderStyle.THIN);   // Viền dưới
        cellStyle.setBorderLeft(BorderStyle.THIN);     // Viền trái
        cellStyle.setBorderRight(BorderStyle.THIN);    //
        // Căn giữa theo chiều ngang và chiều dọc
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setWrapText(true);
        return cellStyle;
    }

    private static CellStyle getCelStyleForData(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setFontName("Arial"); // Thiết lập font là Times New Roman
//        font.setFontHeightInPoints((short) 11);
        CellStyle cellStyle2 = workbook.createCellStyle();
        cellStyle2.setFont(font);
        cellStyle2.setAlignment(HorizontalAlignment.LEFT);
        cellStyle2.setWrapText(true);
        cellStyle2.setBorderTop(BorderStyle.THIN);      // Viền trên
        cellStyle2.setBorderBottom(BorderStyle.THIN);   // Viền dưới
        cellStyle2.setBorderLeft(BorderStyle.THIN);     // Viền trái
        cellStyle2.setBorderRight(BorderStyle.THIN);
        return cellStyle2;
    }
}

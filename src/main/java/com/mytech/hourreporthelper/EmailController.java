package com.mytech.hourreporthelper;

import com.mytech.hourreporthelper.EmailConfig;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

@Controller
@EnableAsync
public class EmailController {

    @Autowired
    private ReportResultService reportResultService;

    @GetMapping("/")
    public String showForm(Model model) {
        System.out.println("showForm server.address=0.0.0.0");
        model.addAttribute("emailConfig", new EmailConfig());
        return "index2";
    }

    @PostMapping("/process")
    public String processForm(@ModelAttribute EmailConfig emailConfig,
            @RequestParam("excelFile") MultipartFile excelFile, Model model) {
        // Log the received data to verify
        System.out.println("SMTP Server: " + emailConfig.getSmtpServer());
        System.out.println("SMTP Port: " + emailConfig.getSmtpPort());
        System.out.println("Username: " + emailConfig.getUsername());
        System.out.println("Recipient: " + emailConfig.getRecipient());

        // Manually set the file in the EmailConfig object
        emailConfig.setExcelFile(excelFile);

        if (excelFile != null && !excelFile.isEmpty()) {
            System.out.println("File name: " + excelFile.getOriginalFilename());
            System.out.println("File size: " + excelFile.getSize());

            // Kiểm tra định dạng file
            if (!excelFile.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
                model.addAttribute("message", "Vui lòng tải lên tệp Excel có định dạng .xlsx!");
                return "result";
            }

            try {
                // Kiểm tra kết nối SMTP trước
                try {
                    boolean smtpConnected = testSmtpConnection(emailConfig);
                    if (!smtpConnected) {
                        model.addAttribute("message",
                                "Không thể kết nối đến máy chủ SMTP. Vui lòng kiểm tra lại thông tin đăng nhập.");
                        return "result";
                    }
                } catch (jakarta.mail.AuthenticationFailedException e) {
                    model.addAttribute("message", "Đăng nhập SMTP không thành công: Sai tên đăng nhập hoặc mật khẩu.");
                    return "result";
                } catch (Exception e) {
                    model.addAttribute("message", "Lỗi kết nối đến máy chủ SMTP: " + e.getMessage());
                    return "result";
                }

                // Xử lý file Excel được tải lên
                processExcelFile(excelFile, emailConfig);

                model.addAttribute("message", "Tệp đã được xử lý thành công! Email đang được gửi trong nền.");
                model.addAttribute("isShowSuccess", true);
                return "result";
            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("message", "Có lỗi xảy ra khi xử lý tệp: " + e.getMessage());
            }
        } else {
            model.addAttribute("message", "Vui lòng tải lên tệp Excel!");
        }

        return "result";
    }

    private String processExcelFile(MultipartFile uploadedFile, EmailConfig emailConfig) throws IOException {
        // Đọc file Excel được tải lên
        Workbook uploadedWorkbook;
        try (InputStream is = uploadedFile.getInputStream()) {
            uploadedWorkbook = WorkbookFactory.create(is);
        }

        // Lấy sheet đầu tiên
        Sheet uploadedSheet = uploadedWorkbook.getSheetAt(0);

        // Lấy thông tin từ file Excel
        String employeeName = emailConfig.getUsername();

        // Lấy ngày báo cáo từ file Excel (giả sử ở ô A2)
        Row dateRow = uploadedSheet.getRow(1); // Row index 1 (second row)
        Cell dateCell = dateRow.getCell(0); // Column A
        LocalDate reportDate;

        if (dateCell != null && dateCell.getCellType() == CellType.STRING) {
            String dateStr = dateCell.getStringCellValue();
            reportDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else {
            reportDate = LocalDate.now(); // Sử dụng ngày hiện tại nếu không tìm thấy
        }

        // Format ngày cho tên file
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMdd");
        String currentDate = reportDate.format(formatter2);

        // Định nghĩa các khung giờ báo cáo
        HashMap<Integer, String> reportTime = new HashMap<>();
        reportTime.put(0, "09");
        reportTime.put(1, "10");
        reportTime.put(2, "11");
        reportTime.put(3, "12");
        reportTime.put(4, "14");
        reportTime.put(5, "15");
        reportTime.put(6, "16");
        reportTime.put(7, "17");

        // Định nghĩa các giờ bắt đầu mặc định
        HashMap<Integer, String> startTime = new HashMap<>();
        startTime.put(0, "08:15");
        startTime.put(1, "09:00");
        startTime.put(2, "10:00");
        startTime.put(3, "11:00");
        startTime.put(4, "13:00");
        startTime.put(5, "14:00");
        startTime.put(6, "15:00");
        startTime.put(7, "16:00");

        // Lấy dữ liệu từ file Excel đã tải lên
        List<String> descriptions = new ArrayList<>();
        List<String> isCompletes = new ArrayList<>();
        List<String> completingTimes = new ArrayList<>();
        List<String> remarks = new ArrayList<>();
        List<String> startTimes = new ArrayList<>();

        // Đọc dữ liệu từ file Excel (bắt đầu từ hàng 2)
        for (int i = 1; i <= 8; i++) {
            Row row = uploadedSheet.getRow(i);
            if (row != null) {
                // Lấy giá trị từ các cột
                startTimes.add(getCellValueAsString(row.getCell(2))); // Cột C - Starting time
                descriptions.add(getCellValueAsString(row.getCell(3))); // Cột D - Job Content
                isCompletes.add(getCellValueAsString(row.getCell(4))); // Cột E - Completed Y/N
                completingTimes.add(getCellValueAsString(row.getCell(5))); // Cột F - Completing time
                remarks.add(getCellValueAsString(row.getCell(6))); // Cột G - Remark
            } else {
                // Nếu không có dữ liệu, thêm giá trị mặc định
                startTimes.add(startTime.get(i - 1));
                descriptions.add("");
                isCompletes.add("N");
                completingTimes.add("");
                remarks.add("");
            }
        }

        // Lấy tên file gốc và tách phần prefix
        String originalFileName = uploadedFile.getOriginalFilename();
        String filePrefix = "";

        if (originalFileName != null && originalFileName.endsWith(".xlsx")) {
            // Tìm vị trí của khung giờ cuối cùng (ví dụ: 17H)
            int hourSuffixIndex = originalFileName.lastIndexOf("_");
            if (hourSuffixIndex > 0) {
                filePrefix = originalFileName.substring(0, hourSuffixIndex + 1);
            } else {
                // Nếu không tìm thấy định dạng chuẩn, tạo prefix mặc định
                filePrefix = "Hour Report_DevHCM_" + employeeName + "_" + currentDate + "_";
            }
        } else {
            // Nếu không có tên file hoặc không phải file Excel, tạo prefix mặc định
            filePrefix = "Hour Report_DevHCM_" + employeeName + "_" + currentDate + "_";
        }

        // Gửi file gốc
        String originalFileFullName = filePrefix + reportTime.get(7) + "H" + ".xlsx";
        File originalFile = File.createTempFile("original_", ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(originalFile)) {
            uploadedWorkbook.write(fos);
        }

        // Lưu thông tin file gốc vào report result
        String originalFileNameWithoutExt = originalFileFullName;
        if (originalFileFullName.toLowerCase().endsWith(".xlsx")) {
            originalFileNameWithoutExt = originalFileFullName.substring(0, originalFileFullName.length() - 5);
        }
        reportResultService.addUploadResult(emailConfig.getUsername(), originalFileNameWithoutExt);

        // Gửi email cho file gốc
        sendEmailAsync(emailConfig, originalFile, originalFileFullName);

        // Tạo và gửi 7 file Excel còn lại
        for (int i = 0; i < 7; i++) {
            if (descriptions.get(i).isEmpty())
                continue;

            String fileName = filePrefix + reportTime.get(i) + "H" + ".xlsx";
            File tempFile = File.createTempFile("report_" + i + "_", ".xlsx");

            try (XSSFWorkbook workbook = new XSSFWorkbook();
                    FileOutputStream fos = new FileOutputStream(tempFile)) {

                Sheet sheet = workbook.createSheet("Sheet 1");

                // Tạo header
                Row headerRow = sheet.createRow(0);
                CellStyle cellStyle = getCellStyleForHeader(workbook);

                handleCellString(headerRow, 0, cellStyle, "ReportDate");
                handleCellString(headerRow, 1, cellStyle, "Report time");
                handleCellString(headerRow, 2, cellStyle, "Starting time");
                handleCellString(headerRow, 3, cellStyle, "Job Content");
                handleCellString(headerRow, 4, cellStyle, "Completed Y/N");
                handleCellString(headerRow, 5, cellStyle, "Completing time");
                handleCellString(headerRow, 6, cellStyle, "Remark");

                // Thiết lập độ rộng cột
                sheet.setColumnWidth(0, 3200);
                sheet.setColumnWidth(1, 3200);
                sheet.setColumnWidth(2, 3100);
                sheet.setColumnWidth(3, 26000);
                sheet.setColumnWidth(4, 4000);
                sheet.setColumnWidth(5, 4000);
                sheet.setColumnWidth(6, 15000);

                // Thêm dữ liệu
                for (int rowNum = 0; rowNum <= i; rowNum++) {
                    Row row = sheet.createRow(rowNum + 1);
                    row.setHeight((short) -1);

                    CellStyle cellStyle2 = getCelStyleForData(workbook);

                    Cell cellR0 = row.createCell(0);
                    cellR0.setCellStyle(cellStyle);
                    cellR0.setCellValue(reportDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

                    Cell cellR1 = row.createCell(1);
                    cellR1.setCellStyle(cellStyle);
                    cellR1.setCellValue(reportTime.get(rowNum));

                    Cell cellR2 = row.createCell(2);
                    cellR2.setCellStyle(cellStyle);
                    cellR2.setCellValue(
                            startTimes.get(rowNum).isEmpty() ? startTime.get(rowNum) : startTimes.get(rowNum));

                    Cell jobContentCell = row.createCell(3);
                    jobContentCell.setCellStyle(cellStyle2);
                    jobContentCell.setCellValue(descriptions.get(rowNum));

                    Cell cellComplete = row.createCell(4);
                    cellComplete.setCellStyle(cellStyle);
                    cellComplete.setCellValue(isCompletes.get(rowNum));

                    Cell cellR5 = row.createCell(5);
                    cellR5.setCellStyle(cellStyle2);
                    cellR5.setCellValue(completingTimes.get(rowNum).isEmpty() ? "" : completingTimes.get(rowNum));

                    Cell cellR6 = row.createCell(6);
                    cellR6.setCellStyle(cellStyle2);
                    cellR6.setCellValue(remarks.get(rowNum));
                }

                workbook.write(fos);

                // Gửi email cho mỗi file được tạo
                sendEmailAsync(emailConfig, tempFile, fileName);
            }
        }

        return currentDate;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private static Cell handleCellString(Row headerRow, int col, CellStyle cellStyle, String value) {
        Cell cell = headerRow.createCell(col);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(value);
        return cell;
    }

    private static CellStyle getCellStyleForHeader(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setWrapText(true);
        return cellStyle;
    }

    private static CellStyle getCelStyleForData(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setFontName("Arial");
        CellStyle cellStyle2 = workbook.createCellStyle();
        cellStyle2.setFont(font);
        cellStyle2.setAlignment(HorizontalAlignment.LEFT);
        cellStyle2.setWrapText(true);
        cellStyle2.setBorderTop(BorderStyle.THIN);
        cellStyle2.setBorderBottom(BorderStyle.THIN);
        cellStyle2.setBorderLeft(BorderStyle.THIN);
        cellStyle2.setBorderRight(BorderStyle.THIN);
        return cellStyle2;
    }

    /**
     * Gửi email bất đồng bộ với file đính kèm
     */
    @Async
    private CompletableFuture<Void> sendEmailAsync(EmailConfig emailConfig, File attachment, String fileName) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendEmail(emailConfig, attachment, fileName);
            } catch (jakarta.mail.AuthenticationFailedException e) {
                System.err.println("Lỗi xác thực khi gửi email cho file " + fileName + ": " + e.getMessage());
            } catch (MessagingException e) {
                System.err.println("Lỗi khi gửi email cho file " + fileName + ": " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Lỗi không xác định khi gửi email cho file " + fileName + ": " + e.getMessage());
            }
        });
    }

    /**
     * Gửi email với file đính kèm
     */
    private void sendEmail(EmailConfig emailConfig, File attachment, String fileName) throws MessagingException {
        // Tạo JavaMailSender từ thông tin cấu hình
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(emailConfig.getSmtpServer());
        mailSender.setPort(Integer.parseInt(emailConfig.getSmtpPort()));

        // Sử dụng email đầy đủ làm username thay vì chỉ lấy phần local
        String username = emailConfig.getUsername();
        mailSender.setUsername(username);
        mailSender.setPassword(emailConfig.getPassword());

        // Cấu hình SSL/TLS và các thuộc tính khác
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "false"); // Tắt STARTTLS vì đã dùng SSL
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.port", emailConfig.getSmtpPort());

        // Cấu hình đặc biệt cho Hanbiro Groupware
        props.put("mail.smtp.from", username);
        props.put("mail.smtp.sendpartial", "true");
        props.put("mail.smtp.userset", "true");
        props.put("mail.store.protocol", "imaps");

        // Tắt chế độ debug để tránh log nhiều
        props.put("mail.debug", "false");
        props.put("mail.debug.auth", "false");

        try {
            // Tạo Session riêng để có thể cấu hình lưu email đã gửi
            jakarta.mail.Session session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new jakarta.mail.PasswordAuthentication(username, emailConfig.getPassword());
                }
            });
            session.setDebug(false);

            // Cấu hình cơ bản
            session.getProperties().put("mail.smtp.sendpartial", "true");

            // Sử dụng session đã cấu hình
            mailSender.setSession(session);

            // Tạo email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Sử dụng định dạng "Tên <email>" để hiển thị tên người gửi
            helper.setFrom(username, emailConfig.getDisplayName());
            helper.setTo(emailConfig.getRecipient());

            // Thêm CC cho người gửi để theo dõi email đã gửi
            helper.setCc(username);

            // Loại bỏ đuôi .xlsx khỏi subject
            String subject = fileName;
            if (fileName.toLowerCase().endsWith(".xlsx")) {
                subject = fileName.substring(0, fileName.length() - 5);
            }
            helper.setSubject(subject);

            helper.setText(""); // Nội dung email để trống

            // Đính kèm file
            FileSystemResource file = new FileSystemResource(attachment);
            helper.addAttachment(fileName, file);

            // Gửi email
            mailSender.send(message);

            System.out.println("Đã gửi email cho file: " + fileName);
        } catch (jakarta.mail.AuthenticationFailedException e) {
            System.err.println("Lỗi xác thực khi gửi email: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email cho file " + fileName + ": " + e.getMessage());
            // Throw as MessagingException so that it can be handled by the calling method
            throw new MessagingException("Lỗi gửi email: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra kết nối đến máy chủ SMTP
     */
    private boolean testSmtpConnection(EmailConfig emailConfig) throws jakarta.mail.AuthenticationFailedException {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(emailConfig.getSmtpServer());
        mailSender.setPort(Integer.parseInt(emailConfig.getSmtpPort()));

        String username = emailConfig.getUsername();
        mailSender.setUsername(username);
        mailSender.setPassword(emailConfig.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.port", emailConfig.getSmtpPort());

        // Minimize debug output to avoid excessive logging
        props.put("mail.debug", "false");
        props.put("mail.debug.auth", "false");

        try {
            mailSender.testConnection();
            System.out.println("Kết nối SMTP thành công!");
            return true;
        } catch (jakarta.mail.AuthenticationFailedException e) {
            System.err.println("Lỗi xác thực SMTP: " + e.getMessage());
            // Rethrow to be handled by the calling method
            throw e;
        } catch (Exception e) {
            System.err.println("Lỗi kết nối SMTP: " + e.getMessage());
            return false;
        }
    }
}
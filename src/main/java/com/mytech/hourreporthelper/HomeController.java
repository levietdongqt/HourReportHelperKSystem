package com.mytech.hourreporthelper;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class HomeController {
    
    @Autowired
    private ReportResultService reportResultService;
    
    @GetMapping("/")
    public String showCombinedForm(Model model) {
        // Add email config model attribute
        model.addAttribute("emailConfig", new EmailConfig());
        
        // Add report form model attribute
        InputForm inputForm = new InputForm();
        model.addAttribute("form", inputForm);
        
        // Set default values for report
        model.addAttribute("now", LocalDate.now());
        model.addAttribute("isShowSuccess", false);
        
        return "combined-form";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("login", new login());
        return "admin";
    }

    @PostMapping("/submit-combined")
    public String submitCombinedForm(
            @ModelAttribute("emailConfig") EmailConfig emailConfig,
            @ModelAttribute("form") InputForm form,
            @RequestParam(name = "tableData",required = false) String tableDataJson,
            Model model) throws IOException, InterruptedException, ExecutionException {
        
        try {
            // Parse table data from JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode tableData = mapper.readTree(tableDataJson);
            
            // Update form with table data
            form.setStartTimes(new ArrayList<>(Arrays.asList(mapper.convertValue(tableData.get("startTimes"), String[].class))));
            form.setDescriptions(new ArrayList<>(Arrays.asList(mapper.convertValue(tableData.get("descriptions"), String[].class))));
            form.setIsCompletes(new ArrayList<>(Arrays.asList(mapper.convertValue(tableData.get("isCompletes"), String[].class))));
            form.setCompletingTimes(new ArrayList<>(Arrays.asList(mapper.convertValue(tableData.get("completingTimes"), String[].class))));
            form.setRemarks(new ArrayList<>(Arrays.asList(mapper.convertValue(tableData.get("remarks"), String[].class))));

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
            
            // Tạo các file báo cáo Excel
            List<File> createdFiles = createReportFiles(form);
            
            // Gửi email với các file báo cáo
            try {
                long startTime = System.currentTimeMillis();
                sendEmailsWithReports(emailConfig, createdFiles, form);
                long endTime = System.currentTimeMillis();
                
                double processingTimeInSeconds = (endTime - startTime) / 1000.0;
                
                model.addAttribute("message", String.format(
                    "Tệp đã được xử lý thành công và tất cả email đã được gửi! (%.1f giây)", 
                    processingTimeInSeconds));
                model.addAttribute("isShowSuccess", true);
            } catch (ExecutionException e) {
                model.addAttribute("message", e.getCause().getMessage());
            } catch (InterruptedException e) {
                model.addAttribute("message", "Quá trình gửi email bị gián đoạn: " + e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                model.addAttribute("message", "Lỗi không xác định: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Xóa các file tạm
                for (File file : createdFiles) {
                    try {
                        if (file != null && file.exists()) {
                            boolean deleted = file.delete();
                            if (!deleted) {
                                System.err.println("Không thể xóa file tạm: " + file.getAbsolutePath());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi khi xóa file tạm: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return "result";
    }

    // Phương thức để tạo các file báo cáo Excel và trả về danh sách các file đã tạo
    private List<File> createReportFiles(InputForm form) throws IOException {
        List<File> createdFiles = new ArrayList<>();
        
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
        
        // Sử dụng thư mục tạm của hệ thống để tạo file
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileNameTemp = "Hour Report_" + form.getDept() + "_" + form.getEmployeeName() + "_" + currentDate + "_";

        for (int i = 0; i < 8; i++) {
            if (form.getDescriptions().get(i).isEmpty()) continue;

            String fileName = fileNameTemp + reportTime.get(i) + "H" + ".xlsx";
            File file = File.createTempFile("report_" + i + "_", ".xlsx");
            createdFiles.add(file);

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

                sheet.setColumnWidth(0, 3200);
                sheet.setColumnWidth(1, 3200);
                sheet.setColumnWidth(2, 3100);
                sheet.setColumnWidth(3, 26000);
                sheet.setColumnWidth(4, 4000);
                sheet.setColumnWidth(5, 4000);
                sheet.setColumnWidth(6, 15000);
                
                for (int rowNum = 0; rowNum <= i; rowNum++) {
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
                    cellR2.setCellValue(form.startTimes.get(rowNum).isEmpty() ? startTime.get(rowNum) : form.startTimes.get(rowNum));

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

        return createdFiles;
    }
    
    // Phương thức để gửi email với các file báo cáo
    private void sendEmailsWithReports(EmailConfig emailConfig, List<File> reportFiles, InputForm form) 
            throws ExecutionException, InterruptedException {
        // Tạo danh sách để lưu trữ tất cả các CompletableFuture
        List<CompletableFuture<Void>> allEmailTasks = new ArrayList<>();
        // Lưu danh sách tên file đã xử lý để báo cáo lỗi chi tiết nếu có
        List<String> processedFileNames = new ArrayList<>();
        
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMdd");
        String currentDate = form.getReportDate().format(formatter2);
        String fileNamePrefix = "Hour Report_" + form.getDept() + "_" + form.getEmployeeName() + "_" + currentDate + "_";
        
        HashMap<Integer, String> reportTime = new HashMap<>();
        reportTime.put(0, "09");
        reportTime.put(1, "10");
        reportTime.put(2, "11");
        reportTime.put(3, "12");
        reportTime.put(4, "14");
        reportTime.put(5, "15");
        reportTime.put(6, "16");
        reportTime.put(7, "17");
        
        try {
            int index = 0;
            for (File file : reportFiles) {
                String reportHour = reportTime.get(index);
                String fileName = fileNamePrefix + reportHour + "H.xlsx";
                
                // Lưu thông tin file vào report result
                String fileNameWithoutExt = fileName;
                if (fileName.toLowerCase().endsWith(".xlsx")) {
                    fileNameWithoutExt = fileName.substring(0, fileName.length() - 5);
                }
                reportResultService.addUploadResult(emailConfig.getUsername(), fileNameWithoutExt);
                
                // Gửi email với file báo cáo
                CompletableFuture<Void> emailTask = sendEmailAsync(emailConfig, file, fileName);
                allEmailTasks.add(emailTask);
                processedFileNames.add(fileName);
                
                index++;
            }
            
            // Đợi tất cả các email hoàn thành
            CompletableFuture<Void> allTasksFuture = CompletableFuture.allOf(
                allEmailTasks.toArray(new CompletableFuture[0])
            );
            
            // Chờ tất cả các email gửi xong
            allTasksFuture.get();
            
            System.out.println("Tất cả các email đã được gửi thành công!");
        } catch (ExecutionException e) {
            // Khi một trong các task bị lỗi
            StringBuilder errorMessage = new StringBuilder("Lỗi khi gửi một số email: ");
            
            // Kiểm tra từng task để xem cái nào bị lỗi
            for (int i = 0; i < allEmailTasks.size(); i++) {
                CompletableFuture<Void> task = allEmailTasks.get(i);
                String fileName = processedFileNames.get(i);
                
                try {
                    // Nếu task đã hoàn thành, điều này sẽ không ném ngoại lệ
                    if (task.isDone() && !task.isCompletedExceptionally()) {
                        continue;
                    }
                    
                    // Nếu task bị lỗi, điều này sẽ ném ngoại lệ
                    task.getNow(null);
                } catch (Exception ex) {
                    errorMessage.append("\n- File '").append(fileName).append("': ").append(ex.getMessage());
                }
            }
            
            // Ném ngoại lệ với thông tin chi tiết
            throw new ExecutionException(errorMessage.toString(), e.getCause());
        }
    }
    
    // Các phương thức hỗ trợ cho việc gửi email
    
    @org.springframework.scheduling.annotation.Async
    private CompletableFuture<Void> sendEmailAsync(EmailConfig emailConfig, File attachment, String fileName) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            // Gọi phương thức gửi email đồng bộ
            sendEmail(emailConfig, attachment, fileName);
            future.complete(null); // Hoàn thành thành công
            System.out.println("Email gửi thành công cho file: " + fileName);
        } catch (jakarta.mail.AuthenticationFailedException e) {
            String errorMsg = "Lỗi xác thực khi gửi email cho file " + fileName + ": " + e.getMessage();
            System.err.println(errorMsg);
            future.completeExceptionally(new RuntimeException(errorMsg, e));
        } catch (MessagingException e) {
            String errorMsg = "Lỗi khi gửi email cho file " + fileName + ": " + e.getMessage();
            System.err.println(errorMsg);
            future.completeExceptionally(new RuntimeException(errorMsg, e));
        } catch (Exception e) {
            String errorMsg = "Lỗi không xác định khi gửi email cho file " + fileName + ": " + e.getMessage();
            System.err.println(errorMsg);
            future.completeExceptionally(new RuntimeException(errorMsg, e));
        }
        
        return future;
    }
    
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
            //DONGLV
            mailSender.send(message);
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
}

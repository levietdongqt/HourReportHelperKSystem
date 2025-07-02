package com.mytech.hourreporthelper;

public class EmailConfig {
    // Fix cứng thông tin SMTP server
    private final String smtpServer = "gw.ksystem.vn";
    private final String smtpPort = "465";
    
    private String username;
    private String password;
    private String sender;
    private String recipient;
    private String subject = "Auto Send Report";
    private String message = "Đây là email tự động gửi báo cáo.";
    private String displayName = "Le Viet Dong (HCM)";

    public EmailConfig() {
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 
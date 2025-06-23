package com.mytech.hourreporthelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class ReportResultService {
    // Save report_results.json file in the project directory
    private final String DATA_FILE = "report_results.json";
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private List<ReportResult> reportResults = new ArrayList<>(); // Initialize to prevent NPE
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @PostConstruct
    public void init() {
        try {
            loadData();
            System.out.println("ReportResultService initialized successfully. Data file: " + DATA_FILE);
        } catch (Exception e) {
            System.err.println("Error initializing ReportResultService: " + e.getMessage());
            // Ensure we have a valid list even if loading fails
            reportResults = new ArrayList<>();
        }
    }
    
    private void loadData() {
        try {
            File file = new File(DATA_FILE);
            if (file.exists()) {
                lock.readLock().lock();
                try {
                    reportResults = objectMapper.readValue(file, new TypeReference<List<ReportResult>>() {});
                } finally {
                    lock.readLock().unlock();
                }
            } else {
                // Just initialize an empty list without trying to save immediately
                reportResults = new ArrayList<>();
                // Only try to save if we can create the parent directory
                File parentDir = file.getParentFile();
                if (parentDir != null && (parentDir.exists() || parentDir.mkdirs())) {
                    saveData();
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading data: " + e.getMessage());
            reportResults = new ArrayList<>();
        }
    }
    
    private void saveData() {
        lock.writeLock().lock();
        try {
            objectMapper.writeValue(new File(DATA_FILE), reportResults);
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public List<ReportResult> getAllReportResults() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(reportResults);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void addUploadResult(String username, String fileName) {
        if (username == null || fileName == null || username.isEmpty() || fileName.isEmpty()) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            // Find existing report result with the same username
            ReportResult existingResult = reportResults.stream()
                .filter(result -> result.getId().equals(username))
                .findFirst()
                .orElse(null);
            
            if (existingResult == null) {
                // Create new report result
                existingResult = new ReportResult(username);
                reportResults.add(existingResult);
            }
            
            // Add the file name to the result list
            existingResult.addResult(fileName);
            
            // Save the updated data
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }
} 
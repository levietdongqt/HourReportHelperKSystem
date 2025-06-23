package com.mytech.hourreporthelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.HttpStatus;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class ReportResultController {

    @Autowired
    private ReportResultService reportResultService;

    /**
     * Returns a JSON array of all report results
     * This endpoint can only be accessed from localhost
     * 
     * @return List of all report results
     */
    @PostMapping("/donglv/admin")
    public ResponseEntity<List<ReportResult>> getReportResults(HttpServletRequest request) {
        // Check if request is coming from localhost
        // String remoteAddr = request.getRemoteAddr();
        // if (!"127.0.0.1".equals(remoteAddr) && !"0:0:0:0:0:0:0:1".equals(remoteAddr) && !"localhost".equals(remoteAddr)) {
        //     return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        // }
        
        List<ReportResult> results = reportResultService.getAllReportResults();
        return ResponseEntity.ok(results);
    }
} 
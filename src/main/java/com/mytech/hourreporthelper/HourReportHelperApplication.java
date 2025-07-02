package com.mytech.hourreporthelper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HourReportHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(HourReportHelperApplication.class, args);
    }

}

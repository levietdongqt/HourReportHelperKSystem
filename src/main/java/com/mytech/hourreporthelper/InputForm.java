package com.mytech.hourreporthelper;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Setter
@Getter
public class InputForm {
    public InputForm() {
        descriptions = new ArrayList<>();
        isCompletes = new ArrayList<>();
        remarks = new ArrayList<>();

        reportTime.put(0, "8-9H");
        reportTime.put(1, "9-10H");
        reportTime.put(2, "10-11H");
        reportTime.put(3, "11-12H");
        reportTime.put(4, "13-14H");
        reportTime.put(5, "14-15H");
        reportTime.put(6, "15-16H");
        reportTime.put(7, "16-17H");
        startTimes.addAll(List.of("8:15","","","","","","",""));


        loops.addAll(List.of(0,1,2,3,4,5,6,7));
    }
    HashMap<Integer, String> reportTime = new HashMap<>();
    ArrayList<String> startTimes = new ArrayList<>();
    ArrayList<String> completingTimes = new ArrayList<>();
    private List<Integer>  loops = new ArrayList<>();
    private String employeeName;
    private String dept;
    private List<String> descriptions;
    private List<String> isCompletes;
    private ArrayList<String> remarks;
    private LocalDate reportDate = LocalDate.now();
    private String description_10H;
    private String description_11H;
    private String description_12H;
    private String description_14H;
    private String description_15H;
    private String description_16H;
    private String description_17H;

}

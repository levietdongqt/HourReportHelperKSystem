package com.mytech.hourreporthelper;

import lombok.Getter;
import lombok.Setter;

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

        startTime.put(0, "08-9H");
        startTime.put(1, "09-10H");
        startTime.put(2, "10-11H");
        startTime.put(3, "11-12H");
        startTime.put(4, "13-14H");
        startTime.put(5, "14-15H");
        startTime.put(6, "15H-16H");
        startTime.put(7, "16H-17H");

        loops.addAll(List.of(0,1,2,3,4,5,6,7));
    }
    HashMap<Integer, String> startTime = new HashMap<>();
    ArrayList<String> completingTimes = new ArrayList<>();
    private List<Integer>  loops = new ArrayList<>();
    private String employeeName;
    private List<String> descriptions;
    private List<String> isCompletes;
    private ArrayList<String> remarks;
    private String description_10H;
    private String description_11H;
    private String description_12H;
    private String description_14H;
    private String description_15H;
    private String description_16H;
    private String description_17H;

}

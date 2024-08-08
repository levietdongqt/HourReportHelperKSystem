package com.mytech.hourreporthelper;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class InputForm {
    private String employeeName;
    private List<String> descriptions;
    private String description_10H;
    private String description_11H;
    private String description_12H;
    private String description_14H;
    private String description_15H;
    private String description_16H;
    private String description_17H;

}

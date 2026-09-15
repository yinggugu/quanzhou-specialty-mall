package com.quanzhou.mall.bean;

import lombok.Data;
import java.util.List;

@Data
public class OllamaRequest {
    private String model;
    private List<OllamaMessage> messages;
    private Boolean stream = false;
}

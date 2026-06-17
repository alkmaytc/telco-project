package com.telco.backend.dto;

import lombok.Data;

@Data
public class OrderRequestDTO {
    private String bbk;
    private String packageName;
    private int speedMbps;
    private double price;
}
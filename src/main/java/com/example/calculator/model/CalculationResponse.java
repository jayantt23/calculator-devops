package com.example.calculator.model;

import java.math.BigDecimal;

public record CalculationResponse(String operation, BigDecimal a, BigDecimal b, BigDecimal result) {
}

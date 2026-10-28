package com.example.calculator.service;

import com.example.calculator.exception.DivisionByZeroException;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Service;

@Service
public class CalculatorService {

    public BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b);
    }

    public BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b);
    }

    public BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b);
    }

    public BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (b.signum() == 0) {
            throw new DivisionByZeroException();
        }
        // DECIMAL64 = 16 significant digits; avoids ArithmeticException on non-terminating results (1/3)
        return a.divide(b, MathContext.DECIMAL64);
    }
}

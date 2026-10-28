package com.example.calculator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.calculator.exception.DivisionByZeroException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalculatorServiceTest {

    private final CalculatorService service = new CalculatorService();

    private static void assertNumEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    @ParameterizedTest
    @CsvSource({"2,3,5", "-2,3,1", "0.1,0.2,0.3", "1000000000000,1,1000000000001"})
    void add(String a, String b, String expected) {
        assertNumEquals(expected, service.add(new BigDecimal(a), new BigDecimal(b)));
    }

    @ParameterizedTest
    @CsvSource({"5,3,2", "3,5,-2", "0.3,0.1,0.2"})
    void subtract(String a, String b, String expected) {
        assertNumEquals(expected, service.subtract(new BigDecimal(a), new BigDecimal(b)));
    }

    @ParameterizedTest
    @CsvSource({"4,3,12", "-4,3,-12", "2.5,2,5.0", "0,99,0"})
    void multiply(String a, String b, String expected) {
        assertNumEquals(expected, service.multiply(new BigDecimal(a), new BigDecimal(b)));
    }

    @ParameterizedTest
    @CsvSource({"6,3,2", "10,4,2.5", "-9,3,-3", "1,3,0.3333333333333333"})
    void divide(String a, String b, String expected) {
        assertNumEquals(expected, service.divide(new BigDecimal(a), new BigDecimal(b)));
    }

    @Test
    void divideByZeroThrows() {
        assertThrows(DivisionByZeroException.class,
                () -> service.divide(BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    void divideByZeroWithScaleThrows() {
        assertThrows(DivisionByZeroException.class,
                () -> service.divide(BigDecimal.ONE, new BigDecimal("0.000")));
    }
}

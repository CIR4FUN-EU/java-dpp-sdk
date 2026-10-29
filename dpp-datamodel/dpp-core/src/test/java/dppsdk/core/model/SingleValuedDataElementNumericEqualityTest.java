package dppsdk.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SingleValuedDataElementNumericEqualityTest {
    @Test
    void treatsJsonEquivalentIntegralAndDecimalNumbersAsEqual() {
        assertEquals(single(42), single(42L));
        assertEquals(single(new BigInteger("42")), single(42));
        assertEquals(single(new BigDecimal("25.50")), single(25.5d));
        assertEquals(single(new BigDecimal("25.500")), single(new BigDecimal("25.5")));
    }

    @Test
    void equalJsonNumbersHaveEqualHashes() {
        assertEquals(single(42L).hashCode(), single(new BigInteger("42")).hashCode());
        assertEquals(single(new BigDecimal("25.50")).hashCode(), single(25.5d).hashCode());
    }

    private static SingleValuedDataElement single(Object value) {
        return new SingleValuedDataElement.Builder().elementId("number").value(value).build();
    }
}

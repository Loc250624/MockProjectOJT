package com.ojtsu26.elearning.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VnpayCallbackUtilsTest {

    @Test
    void normalizesSuccessfulCallbackOnlyWhenBothStatusesAreSuccessful() {
        Map<String, String> normalized = VnpayCallbackUtils.normalize(Map.of(
                "vnp_TxnRef", "ORD-100",
                "vnp_TransactionNo", "14500123",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00",
                "vnp_Amount", "1000000"
        ));

        assertEquals("ORD-100", normalized.get("orderId"));
        assertEquals("14500123", normalized.get("transId"));
        assertEquals("0", normalized.get("resultCode"));
        assertEquals("1000000", normalized.get("vnp_Amount"));
        assertTrue(VnpayCallbackUtils.isSuccessful(normalized));
    }

    @Test
    void mapsFailedResponseToFailure() {
        Map<String, String> normalized = VnpayCallbackUtils.normalize(Map.of(
                "vnp_TxnRef", "ORD-101",
                "vnp_TransactionNo", "0",
                "vnp_ResponseCode", "24",
                "vnp_TransactionStatus", "02"
        ));

        assertEquals("24", normalized.get("resultCode"));
        assertFalse(VnpayCallbackUtils.isSuccessful(normalized));
    }

    @Test
    void doesNotTreatMissingTransactionStatusAsSuccess() {
        Map<String, String> normalized = VnpayCallbackUtils.normalize(Map.of(
                "vnp_TxnRef", "ORD-102",
                "vnp_ResponseCode", "00"
        ));

        assertEquals("99", normalized.get("resultCode"));
        assertFalse(VnpayCallbackUtils.isSuccessful(normalized));
    }
}

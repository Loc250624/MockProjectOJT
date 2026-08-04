package com.ojtsu26.elearning.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes VNPay Return/IPN parameters for PaymentService while retaining
 * every original vnp_ field required by checksum and business validation.
 */
public final class VnpayCallbackUtils {

    private VnpayCallbackUtils() {
    }

    public static Map<String, String> normalize(Map<String, String> rawParams) {
        if (rawParams == null) {
            throw new IllegalArgumentException("VNPay callback parameters are required");
        }

        Map<String, String> normalized = new HashMap<>(rawParams);
        normalized.put("orderId", rawParams.get("vnp_TxnRef"));
        normalized.put("transId", rawParams.get("vnp_TransactionNo"));
        normalized.put("resultCode", isSuccessful(rawParams) ? "0" : failureCode(rawParams));
        return normalized;
    }

    public static boolean isSuccessful(Map<String, String> rawParams) {
        return rawParams != null
                && "00".equals(rawParams.get("vnp_ResponseCode"))
                && "00".equals(rawParams.get("vnp_TransactionStatus"));
    }

    private static String failureCode(Map<String, String> rawParams) {
        String responseCode = rawParams.get("vnp_ResponseCode");
        if (responseCode != null && !responseCode.isBlank() && !"00".equals(responseCode)) {
            return responseCode;
        }

        String transactionStatus = rawParams.get("vnp_TransactionStatus");
        if (transactionStatus != null && !transactionStatus.isBlank() && !"00".equals(transactionStatus)) {
            return transactionStatus;
        }
        return "99";
    }
}

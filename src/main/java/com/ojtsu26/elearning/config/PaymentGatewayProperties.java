package com.ojtsu26.elearning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "payment.gateway")
@Data
public class PaymentGatewayProperties {
    
    private BigDecimal exchangeRate = new BigDecimal("25000");
    private MomoProperties momo = new MomoProperties();
    private VnpayProperties vnpay = new VnpayProperties();

    @Data
    public static class MomoProperties {
        private String partnerCode;
        private String accessKey;
        private String secretKey;
        private String endpoint;
        private String returnUrl;
        private String ipnUrl;
    }

    @Data
    public static class VnpayProperties {
        private String tmnCode;
        private String hashSecret;
        private String endpoint;
        private String returnUrl;
        private String ipnUrl;
    }
}

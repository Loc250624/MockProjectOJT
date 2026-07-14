package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.TransactionRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_payments_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AdminPaymentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User student;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(user("Admin User", "admin.payments@example.com", Role.ADMIN));
        student = userRepository.save(user("Payment Student", "student.payments@example.com", Role.STUDENT));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void paymentsPageUsesRealTransactionSummaryAndNoDemoMoney() throws Exception {
        saveTransaction("PAY-SUCCESS-1", "100000.25", TransactionStatus.SUCCESS);
        saveTransaction("PAY-SUCCESS-2", "25000.50", TransactionStatus.SUCCESS);
        saveTransaction("PAY-PENDING", "999999.00", TransactionStatus.PENDING);
        saveTransaction("PAY-FAILED", "888888.00", TransactionStatus.FAILED);
        saveTransaction("PAY-REFUNDED", "777777.00", TransactionStatus.REFUNDED);

        mockMvc.perform(get("/admin/payments"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("125,000.75 VND")))
                .andExpect(content().string(containsString("2 successful transactions")))
                .andExpect(content().string(containsString("5 total transactions")))
                .andExpect(content().string(containsString("PAY-SUCCESS-1")))
                .andExpect(content().string(not(containsString("$84,210.00"))))
                .andExpect(content().string(not(containsString("$58,340.50"))))
                .andExpect(content().string(not(containsString("Nov 05, 2024"))))
                .andExpect(content().string(not(containsString("Dr. Elena Vance"))))
                .andExpect(content().string(not(containsString("Prof. Sarah Jenkins"))));
    }

    @Test
    void paymentsTemplateDoesNotContainRemovedDemoFinancialData() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/admin/payments.html"));

        assertThat(template)
                .doesNotContain("$84,210.00")
                .doesNotContain("$58,340.50")
                .doesNotContain("Nov 05, 2024")
                .doesNotContain("Dr. Elena Vance")
                .doesNotContain("Prof. Sarah Jenkins")
                .doesNotContain("[ Bar Chart: Monthly Payouts vs Commission ]")
                .doesNotContain("2024-11-01");
    }

    @Test
    void legacyStudentStatisticsTemplateDoesNotContainDemoCounters() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/admin/student-statistics.html"));

        assertThat(template)
                .doesNotContain("<div class=\"value\">100</div>")
                .doesNotContain("<div class=\"value\">80</div>")
                .doesNotContain("<div class=\"value\">15</div>")
                .doesNotContain("Chart placeholder");
    }

    private void saveTransaction(String ref, String amount, TransactionStatus status) {
        transactionRepository.save(Transaction.builder()
                .student(student)
                .amount(new BigDecimal(amount))
                .paymentMethod(PaymentMethod.MOMO)
                .transactionRef(ref)
                .status(status)
                .build());
    }

    private User user(String fullName, String email, Role role) {
        return User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash("secret")
                .avatarUrl(null)
                .role(role)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }
}

package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.OrderRepository;
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
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_transactions_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AdminTransactionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User student;
    private Course course;
    private Order order;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        orderRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        User teacher = userRepository.save(user("Teacher One", "teacher.transactions@example.com", Role.TEACHER));
        student = userRepository.save(user("Transaction Student", "student.transactions@example.com", Role.STUDENT));
        userRepository.save(user("Admin User", "admin.transactions@example.com", Role.ADMIN));
        course = courseRepository.save(Course.builder()
                .title("Database Transactions")
                .description("Course for transaction tests")
                .price(new BigDecimal("100000"))
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .build());
        order = orderRepository.save(Order.builder()
                .orderCode("ORD-TXN-001")
                .user(student)
                .totalAmount(new BigDecimal("100000"))
                .paidAmount(new BigDecimal("100000"))
                .currency("VND")
                .exchangeRate(BigDecimal.ONE)
                .paymentMethod(PaymentMethod.MOMO)
                .status(OrderStatus.PAID)
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminTransactionsPageReturnsOk() throws Exception {
        mockMvc.perform(get("/admin/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"transactions-table-body\"")))
                .andExpect(content().string(containsString("id=\"transactions-mobile-list\"")));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void nonAdminCannotOpenTransactionsPage() throws Exception {
        mockMvc.perform(get("/admin/transactions"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void emptyApiResponseIsPaginatedAndSafe() throws Exception {
        mockMvc.perform(get("/api/admin/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(content().string(not(containsString("webhookResponse"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listsSuccessPendingFailedAndRefundedTransactions() throws Exception {
        saveTransaction("TXN-SUCCESS", new BigDecimal("100000.25"), TransactionStatus.SUCCESS, PaymentMethod.MOMO);
        saveTransaction("TXN-PENDING", new BigDecimal("200000.00"), TransactionStatus.PENDING, PaymentMethod.VNPAY);
        saveTransaction("TXN-FAILED", new BigDecimal("300000.00"), TransactionStatus.FAILED, PaymentMethod.MOMO);
        saveTransaction("TXN-REFUNDED", new BigDecimal("400000.00"), TransactionStatus.REFUNDED, PaymentMethod.VNPAY);

        mockMvc.perform(get("/api/admin/transactions").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(content().string(containsString("TXN-SUCCESS")))
                .andExpect(content().string(containsString("TXN-PENDING")))
                .andExpect(content().string(containsString("TXN-FAILED")))
                .andExpect(content().string(containsString("TXN-REFUNDED")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void filtersByKeywordStatusPaymentMethodAndDateRange() throws Exception {
        saveTransaction("REF-MOMO-SUCCESS", new BigDecimal("100000.00"), TransactionStatus.SUCCESS, PaymentMethod.MOMO);
        saveTransaction("REF-VNPAY-PENDING", new BigDecimal("200000.00"), TransactionStatus.PENDING, PaymentMethod.VNPAY);

        String today = LocalDate.now().toString();

        mockMvc.perform(get("/api/admin/transactions")
                        .param("keyword", "ORD-TXN-001")
                        .param("status", "SUCCESS")
                        .param("paymentMethod", "MOMO")
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].transactionRef").value("REF-MOMO-SUCCESS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sortsAndPaginatesInBackend() throws Exception {
        saveTransaction("TXN-LOW", new BigDecimal("10.00"), TransactionStatus.SUCCESS, PaymentMethod.MOMO);
        saveTransaction("TXN-HIGH", new BigDecimal("20.00"), TransactionStatus.SUCCESS, PaymentMethod.MOMO);

        mockMvc.perform(get("/api/admin/transactions")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sortBy", "amount")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].transactionRef").value("TXN-HIGH"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidDateRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/transactions")
                        .param("from", "2026-07-15")
                        .param("to", "2026-07-14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be before or equal to to"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidDateFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/transactions").param("from", "07/14/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must use yyyy-MM-dd"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void nonAdminCannotCallTransactionApi() throws Exception {
        mockMvc.perform(get("/api/admin/transactions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void detailIdorIsBlockedForNonAdmin() throws Exception {
        Transaction transaction = saveTransaction("TXN-IDOR", new BigDecimal("10.00"), TransactionStatus.SUCCESS, PaymentMethod.MOMO);

        mockMvc.perform(get("/api/admin/transactions/{id}", transaction.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void detailIsNullSafeAndDoesNotExposeSensitiveFields() throws Exception {
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .amount(new BigDecimal("12.00"))
                .paymentMethod(null)
                .transactionRef("TXN-NULL-SAFE")
                .status(TransactionStatus.SUCCESS)
                .webhookResponse("{\"token\":\"SECRET_GATEWAY_TOKEN\",\"rawPayload\":true}")
                .student(null)
                .course(null)
                .order(null)
                .build());

        mockMvc.perform(get("/api/admin/transactions/{id}", transaction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionRef").value("TXN-NULL-SAFE"))
                .andExpect(jsonPath("$.data.studentName").doesNotExist())
                .andExpect(jsonPath("$.data.courseTitle").doesNotExist())
                .andExpect(content().string(not(containsString("webhookResponse"))))
                .andExpect(content().string(not(containsString("SECRET_GATEWAY_TOKEN"))))
                .andExpect(content().string(not(containsString("rawPayload"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminTransactionsPresentGatewayAmountsAsVndWithoutUsdCurrencyLabel() throws Exception {
        order = orderRepository.save(Order.builder()
                .orderCode("ORD-TXN-VND")
                .user(student)
                .totalAmount(new BigDecimal("129.99"))
                .paidAmount(new BigDecimal("3249750"))
                .currency("USD")
                .exchangeRate(new BigDecimal("25000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .status(OrderStatus.PAID)
                .items(new java.util.ArrayList<>())
                .build());
        Transaction transaction = saveTransaction("TXN-VND", new BigDecimal("3249750"), TransactionStatus.SUCCESS, PaymentMethod.VNPAY);

        mockMvc.perform(get("/api/admin/transactions").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].currency").value("VND"))
                .andExpect(jsonPath("$.data.content[0].amount").value(3249750));

        mockMvc.perform(get("/api/admin/transactions/{id}", transaction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.orderTotalAmount").value(3249750))
                .andExpect(jsonPath("$.data.orderPaidAmount").value(3249750));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listDoesNotReturnDuplicates() throws Exception {
        Transaction one = saveTransaction("TXN-DUP-1", new BigDecimal("10.00"), TransactionStatus.SUCCESS, PaymentMethod.MOMO);
        Transaction two = saveTransaction("TXN-DUP-2", new BigDecimal("20.00"), TransactionStatus.SUCCESS, PaymentMethod.MOMO);

        String response = mockMvc.perform(get("/api/admin/transactions")
                        .param("keyword", "student.transactions@example.com")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Set<Integer> expectedIds = new HashSet<>(Set.of(one.getId(), two.getId()));
        assertThat(response).contains(one.getTransactionRef(), two.getTransactionRef());
        assertThat(response.split("TXN-DUP-1", -1).length - 1).isEqualTo(1);
        assertThat(response.split("TXN-DUP-2", -1).length - 1).isEqualTo(1);
        assertThat(expectedIds).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void summaryCountsOnlySuccessfulAmountAsRevenue() throws Exception {
        saveTransaction("TXN-SUM-SUCCESS", new BigDecimal("100.00"), TransactionStatus.SUCCESS, PaymentMethod.MOMO);
        saveTransaction("TXN-SUM-PENDING", new BigDecimal("200.00"), TransactionStatus.PENDING, PaymentMethod.MOMO);
        saveTransaction("TXN-SUM-FAILED", new BigDecimal("300.00"), TransactionStatus.FAILED, PaymentMethod.MOMO);
        saveTransaction("TXN-SUM-REFUNDED", new BigDecimal("400.00"), TransactionStatus.REFUNDED, PaymentMethod.MOMO);

        mockMvc.perform(get("/api/admin/transactions/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTransactions").value(4))
                .andExpect(jsonPath("$.data.successfulTransactions").value(1))
                .andExpect(jsonPath("$.data.pendingTransactions").value(1))
                .andExpect(jsonPath("$.data.failedTransactions").value(1))
                .andExpect(jsonPath("$.data.refundedTransactions").value(1))
                .andExpect(jsonPath("$.data.successfulAmount").value(100.00));
    }

    @Test
    void transactionsTemplateAndScriptExposeResponsiveRuntimeHooksOnly() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/admin/transactions.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/admin/admin.js"));

        assertThat(template)
                .contains("admin-transactions-mobile-list")
                .contains("transaction-panel")
                .contains("transactions-loading")
                .contains("transactions-empty-state")
                .contains("transactions-error")
                .doesNotContain("webhookResponse")
                .doesNotContain("card PAN")
                .doesNotContain("CVV");
        assertThat(script)
                .contains("initAdminTransactions")
                .contains("/api/admin/transactions")
                .contains("currency: 'VND'")
                .contains("maximumFractionDigits: 0")
                .doesNotContain("webhookResponse")
                .doesNotContain(".00 USD")
                .doesNotContain("rawPayload");
    }

    private Transaction saveTransaction(String ref, BigDecimal amount, TransactionStatus status, PaymentMethod method) {
        return transactionRepository.save(Transaction.builder()
                .student(student)
                .course(course)
                .order(order)
                .amount(amount)
                .paymentMethod(method)
                .transactionRef(ref)
                .status(status)
                .webhookResponse("{\"gateway\":\"test\"}")
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

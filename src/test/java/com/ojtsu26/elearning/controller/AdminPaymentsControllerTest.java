package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.OrderItem;
import com.ojtsu26.elearning.model.entity.SystemSetting;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SystemSettingType;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.OrderRepository;
import com.ojtsu26.elearning.repository.SystemSettingRepository;
import com.ojtsu26.elearning.repository.TransactionRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User student;
    private Order order;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        transactionRepository.deleteAll();
        orderRepository.deleteAll();
        courseRepository.deleteAll();
        systemSettingRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(user("Admin User", "admin.payments@example.com", Role.ADMIN));
        student = userRepository.save(user("Payment Student", "student.payments@example.com", Role.STUDENT));
        order = orderRepository.save(order("ORD-PAY-001", student));
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
    @WithMockUser(roles = "ADMIN")
    void paymentsPageDoesNotExposePdfSettlementQueueOrUnsupportedPayoutAction() throws Exception {
        mockMvc.perform(get("/admin/payments"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(">PDF<"))))
                .andExpect(content().string(not(containsString("Settlement Queue"))))
                .andExpect(content().string(not(containsString("No settlement source configured."))))
                .andExpect(content().string(not(containsString("Process All Payouts"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void teacherPayoutsShowsUnpaidReadinessFromPaidOrderItemsAndConfiguredShare() throws Exception {
        systemSettingRepository.save(SystemSetting.builder()
                .key("commerce.teacherCommissionRate")
                .value("0.60")
                .type(SystemSettingType.DECIMAL)
                .category("Commerce")
                .description("Teacher commission rate as a decimal fraction.")
                .editable(true)
                .build());
        User teacher = userRepository.save(user("Teacher Revenue", "teacher.revenue@example.com", Role.TEACHER));
        User failedOnlyTeacher = userRepository.save(user("Failed Teacher", "failed.teacher@example.com", Role.TEACHER));
        Course paidCourse = courseRepository.save(course("Paid Course", teacher, "100.00"));
        Course failedCourse = courseRepository.save(course("Failed Course", failedOnlyTeacher, "500.00"));

        saveOrderItem("ORD-TEACHER-PAID", student, paidCourse, OrderStatus.PAID, "100.00",
                LocalDateTime.of(2026, 8, 10, 10, 0));
        saveOrderItem("ORD-TEACHER-PENDING", student, paidCourse, OrderStatus.PENDING, "999.00",
                LocalDateTime.of(2026, 8, 10, 11, 0));
        saveOrderItem("ORD-TEACHER-FAILED", student, failedCourse, OrderStatus.FAILED, "500.00",
                LocalDateTime.of(2026, 8, 10, 12, 0));
        saveOrderItem("ORD-TEACHER-OTHER-DAY", student, paidCourse, OrderStatus.PAID, "200.00",
                LocalDateTime.of(2026, 8, 11, 10, 0));

        String html = mockMvc.perform(get("/admin/payments")
                        .param("tab", "teacher")
                        .param("date", "2026-08-10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(html)
                .contains("Teacher Earnings Readiness")
                .contains("Teacher Revenue")
                .contains("teacher.revenue@example.com")
                .contains("2,500,000.00 VND")
                .contains("1,500,000.00 VND")
                .contains("Unpaid")
                .doesNotContain("Failed Teacher")
                .doesNotContain("24,975,000.00 VND")
                .doesNotContain("5,000,000.00 VND")
                .doesNotContain(">Paid</span>");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPayments_withoutDate_returnsDefaultDataset() throws Exception {
        saveTransaction("PAY-DEFAULT-1", "100.00", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 10, 9, 0));
        saveTransaction("PAY-DEFAULT-2", "200.00", TransactionStatus.PENDING,
                LocalDateTime.of(2026, 8, 11, 9, 0));

        mockMvc.perform(get("/admin/payments"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PAY-DEFAULT-1")))
                .andExpect(content().string(containsString("PAY-DEFAULT-2")))
                .andExpect(content().string(containsString("100.00 VND")))
                .andExpect(content().string(containsString("1 successful transactions")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPayments_withDate_filtersExactDayAndUpdatesSummary() throws Exception {
        saveTransaction("PAY-BOUNDARY-START", "100.00", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 10, 0, 0));
        saveTransaction("PAY-BOUNDARY-END", "200.00", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 10, 23, 59, 59));
        saveTransaction("PAY-NEXT-DAY", "300.00", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 11, 0, 0));
        saveTransaction("PAY-PREVIOUS-DAY", "400.00", TransactionStatus.FAILED,
                LocalDateTime.of(2026, 8, 9, 23, 59, 59));

        mockMvc.perform(get("/admin/payments").param("date", "2026-08-10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PAY-BOUNDARY-START")))
                .andExpect(content().string(containsString("PAY-BOUNDARY-END")))
                .andExpect(content().string(not(containsString("PAY-NEXT-DAY"))))
                .andExpect(content().string(not(containsString("PAY-PREVIOUS-DAY"))))
                .andExpect(content().string(containsString("300.00 VND")))
                .andExpect(content().string(containsString("2 successful transactions")))
                .andExpect(content().string(containsString("value=\"2026-08-10\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPayments_withDateWithoutDataShowsEmptyScopedSummary() throws Exception {
        saveTransaction("PAY-OTHER-DAY", "100.00", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 10, 12, 0));

        mockMvc.perform(get("/admin/payments").param("date", "2026-08-12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No transactions found.")))
                .andExpect(content().string(containsString("0.00 VND")))
                .andExpect(content().string(containsString("0 successful transactions")))
                .andExpect(content().string(containsString("0 total transactions")))
                .andExpect(content().string(not(containsString("PAY-OTHER-DAY"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPayments_invalidDateDoesNotReturn500() throws Exception {
        saveTransaction("PAY-INVALID-DATE-FALLBACK", "100.00", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 10, 12, 0));

        mockMvc.perform(get("/admin/payments").param("date", "08/10/2026"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid date filter ignored")))
                .andExpect(content().string(containsString("PAY-INVALID-DATE-FALLBACK")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportCsv_withoutDate_downloadsCsvWithAllRows() throws Exception {
        saveTransaction("PAY-CSV-DEFAULT-1", "100.00", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 10, 9, 0));
        saveTransaction("PAY-CSV-DEFAULT-2", "200.00", TransactionStatus.FAILED,
                LocalDateTime.of(2026, 8, 11, 9, 0));

        String csv = mockMvc.perform(get("/admin/payments/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"lumina-platform-payments.csv\""))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv)
                .startsWith("\uFEFFTransaction ID,Order ID,Customer Name,Customer Email,Amount,Currency,Payment Method,Status,Transaction Date/Time")
                .contains("PAY-CSV-DEFAULT-1")
                .contains("PAY-CSV-DEFAULT-2")
                .contains("100.00,\"VND\"")
                .contains("200.00,\"VND\"");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportCsv_withDate_exportsOnlySelectedDay() throws Exception {
        saveTransaction("PAY-CSV-START", "100.00", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 10, 0, 0));
        saveTransaction("PAY-CSV-END", "200.00", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 10, 23, 59, 59));
        saveTransaction("PAY-CSV-NEXT", "300.00", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 11, 0, 0));

        String csv = mockMvc.perform(get("/admin/payments/export/csv").param("date", "2026-08-10"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"lumina-platform-payments-2026-08-10.csv\""))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv)
                .contains("PAY-CSV-START")
                .contains("PAY-CSV-END")
                .contains("2026-08-10 00:00:00")
                .contains("2026-08-10 23:59:59")
                .doesNotContain("PAY-CSV-NEXT")
                .doesNotContain("2026-08-11 00:00:00");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportCsv_usesTransactionUpdatedTimeWhenCreatedTimeIsMissing() throws Exception {
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .student(student)
                .order(order)
                .amount(new BigDecimal("150000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionRef("PAY-CSV-TXN-TIME-FALLBACK")
                .status(TransactionStatus.SUCCESS)
                .build());
        LocalDateTime transactionUpdatedAt = LocalDateTime.of(2026, 8, 10, 14, 35, 20);
        jdbcTemplate.update("update transactions set created_at = null, updated_at = ? where id = ?",
                Timestamp.valueOf(transactionUpdatedAt), transaction.getId());

        String csv = mockMvc.perform(get("/admin/payments/export/csv"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv)
                .contains("PAY-CSV-TXN-TIME-FALLBACK")
                .contains("2026-08-10 14:35:20");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportCsv_handlesUtf8EscapingAndFormulaInjection() throws Exception {
        User vietnameseStudent = userRepository.save(user("M\u1ea1nh C\u01b0\u1eddng Ho\u00e0ng", "=sheet@example.com", Role.STUDENT));
        Order csvOrder = orderRepository.save(order("ORD,\"CSV\"", vietnameseStudent));
        saveTransaction("=TXN,\"CSV\"", "3249750", TransactionStatus.SUCCESS,
                LocalDateTime.of(2026, 8, 10, 10, 30), vietnameseStudent, csvOrder);

        String csv = mockMvc.perform(get("/admin/payments/export/csv").param("date", "2026-08-10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv)
                .startsWith("\uFEFF")
                .contains("M\u1ea1nh C\u01b0\u1eddng Ho\u00e0ng")
                .contains("\"'=TXN,\"\"CSV\"\"\"")
                .contains("\"ORD,\"\"CSV\"\"\"")
                .contains("\"'=sheet@example.com\"")
                .contains("3249750.00,\"VND\"");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void exportCsv_requiresAdmin() throws Exception {
        mockMvc.perform(get("/admin/payments/export/csv"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
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
        saveTransaction(ref, amount, status, null);
    }

    private void saveTransaction(String ref, String amount, TransactionStatus status, LocalDateTime createdAt) {
        saveTransaction(ref, amount, status, createdAt, student, order);
    }

    private void saveTransaction(String ref, String amount, TransactionStatus status,
                                 LocalDateTime createdAt, User txnStudent, Order txnOrder) {
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .student(txnStudent)
                .order(txnOrder)
                .amount(new BigDecimal(amount))
                .paymentMethod(PaymentMethod.MOMO)
                .transactionRef(ref)
                .status(status)
                .build());
        if (createdAt != null) {
            transaction.setCreatedAt(createdAt);
            transactionRepository.save(transaction);
        }
    }

    private Order order(String orderCode, User user) {
        return Order.builder()
                .orderCode(orderCode)
                .user(user)
                .totalAmount(new BigDecimal("100000"))
                .paidAmount(new BigDecimal("100000"))
                .currency("VND")
                .exchangeRate(BigDecimal.ONE)
                .paymentMethod(PaymentMethod.MOMO)
                .status(OrderStatus.PAID)
                .items(new java.util.ArrayList<>())
                .build();
    }

    private Course course(String title, User instructor, String price) {
        return Course.builder()
                .title(title)
                .description(title)
                .price(new BigDecimal(price))
                .status(CourseStatus.APPROVED)
                .instructor(instructor)
                .build();
    }

    private void saveOrderItem(String orderCode, User buyer, Course course, OrderStatus status,
                               String amount, LocalDateTime createdAt) {
        Order revenueOrder = orderRepository.save(Order.builder()
                .orderCode(orderCode)
                .user(buyer)
                .totalAmount(new BigDecimal(amount))
                .paidAmount(new BigDecimal(amount))
                .currency("USD")
                .exchangeRate(BigDecimal.ONE)
                .paymentMethod(PaymentMethod.MOMO)
                .status(status)
                .items(new java.util.ArrayList<>())
                .build());
        revenueOrder.setCreatedAt(createdAt);
        revenueOrder = orderRepository.save(revenueOrder);
        orderItemRepository.save(OrderItem.builder()
                .order(revenueOrder)
                .course(course)
                .courseName(course.getTitle())
                .unitPrice(new BigDecimal(amount))
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

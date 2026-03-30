package com.chuber.expenseclaim;

import com.chuber.expenseclaim.entity.AppUser;
import com.chuber.expenseclaim.entity.ExpenseClaim;
import com.chuber.expenseclaim.enums.Category;
import com.chuber.expenseclaim.enums.ClaimStatus;
import com.chuber.expenseclaim.repository.ExpenseClaimRepository;
import com.chuber.expenseclaim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseClaimIntegrationTest {

    private static final String HEADER = "X-Requested-With";
    private static final String HEADER_VALUE = "XMLHttpRequest";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ExpenseClaimRepository claimRepository;

    @Autowired
    UserRepository userRepository;

    private ExpenseClaim pendingClaim;
    private ExpenseClaim approvedClaim;

    @BeforeEach
    void setUp() {
        claimRepository.deleteAll();

        AppUser john = userRepository.findByUsername("john.smith").orElseThrow();
        AppUser jane = userRepository.findByUsername("jane.doe").orElseThrow();

        pendingClaim = new ExpenseClaim();
        pendingClaim.setEmployee(john);
        pendingClaim.setDescription("Train ticket");
        pendingClaim.setAmount(new BigDecimal("45.50"));
        pendingClaim.setExpenseDate(LocalDate.of(2026, 3, 15));
        pendingClaim.setCategory(Category.TRAVEL);
        pendingClaim.setSubmittedAt(LocalDateTime.now());
        pendingClaim = claimRepository.save(pendingClaim);

        approvedClaim = new ExpenseClaim();
        approvedClaim.setEmployee(jane);
        approvedClaim.setDescription("Hotel stay");
        approvedClaim.setAmount(new BigDecimal("120.00"));
        approvedClaim.setExpenseDate(LocalDate.of(2026, 3, 10));
        approvedClaim.setCategory(Category.ACCOMMODATION);
        approvedClaim.setStatus(ClaimStatus.APPROVED);
        approvedClaim.setSubmittedAt(LocalDateTime.now());
        approvedClaim.setDecidedBy(userRepository.findByUsername("mike.approver").orElseThrow());
        approvedClaim.setDecidedAt(LocalDateTime.now());
        approvedClaim = claimRepository.save(approvedClaim);
    }

    @Nested
    class SubmitClaim {

        @Test
        @WithMockUser(username = "john.smith", roles = "EMPLOYEE")
        void validClaim_returns201() throws Exception {
            mockMvc.perform(post("/api/claims")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "description": "Lunch with client",
                                      "amount": 35.00,
                                      "expenseDate": "2026-03-20",
                                      "category": "MEALS"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employee").value("john.smith"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.description").value("Lunch with client"))
                    .andExpect(jsonPath("$.amount").value(35.00))
                    .andExpect(jsonPath("$.submittedAt").exists());
        }

        @Test
        @WithMockUser(username = "john.smith", roles = "EMPLOYEE")
        void missingFields_returns400() throws Exception {
            mockMvc.perform(post("/api/claims")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"description": ""}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "john.smith", roles = "EMPLOYEE")
        void negativeAmount_returns400() throws Exception {
            mockMvc.perform(post("/api/claims")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "description": "Refund?",
                                      "amount": -10.00,
                                      "expenseDate": "2026-03-20",
                                      "category": "OTHER"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "john.smith", roles = "EMPLOYEE")
        void descriptionTooLong_returns400() throws Exception {
            String longDesc = "x".repeat(256);
            mockMvc.perform(post("/api/claims")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "description": "%s",
                                      "amount": 10.00,
                                      "expenseDate": "2026-03-20",
                                      "category": "OTHER"
                                    }
                                    """.formatted(longDesc)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "mike.approver", roles = "APPROVER")
        void approverSubmitsClaim_returns403() throws Exception {
            mockMvc.perform(post("/api/claims")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "description": "Shouldn't work",
                                      "amount": 10.00,
                                      "expenseDate": "2026-03-20",
                                      "category": "OTHER"
                                    }
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ListClaims {

        @Test
        @WithMockUser(username = "john.smith", roles = "EMPLOYEE")
        void employeeSeesOnlyOwnClaims() throws Exception {
            mockMvc.perform(get("/api/claims")
                            .header(HEADER, HEADER_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].employee").value("john.smith"));
        }

        @Test
        @WithMockUser(username = "mike.approver", roles = "APPROVER")
        void approverSeesAllClaims() throws Exception {
            mockMvc.perform(get("/api/claims")
                            .header(HEADER, HEADER_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    class GetClaim {

        @Test
        @WithMockUser(username = "john.smith", roles = "EMPLOYEE")
        void employeeViewsOwnClaim_returns200() throws Exception {
            mockMvc.perform(get("/api/claims/" + pendingClaim.getId())
                            .header(HEADER, HEADER_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Train ticket"));
        }

        @Test
        @WithMockUser(username = "john.smith", roles = "EMPLOYEE")
        void employeeViewsOthersClaim_returns403() throws Exception {
            mockMvc.perform(get("/api/claims/" + approvedClaim.getId())
                            .header(HEADER, HEADER_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "mike.approver", roles = "APPROVER")
        void approverViewsAnyClaim_returns200() throws Exception {
            mockMvc.perform(get("/api/claims/" + pendingClaim.getId())
                            .header(HEADER, HEADER_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "john.smith", roles = "EMPLOYEE")
        void nonExistentClaim_returns404() throws Exception {
            mockMvc.perform(get("/api/claims/99999")
                            .header(HEADER, HEADER_VALUE))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DecideClaim {

        @Test
        @WithMockUser(username = "mike.approver", roles = "APPROVER")
        void approvePendingClaim_returns200() throws Exception {
            mockMvc.perform(put("/api/claims/" + pendingClaim.getId() + "/decision")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status": "APPROVED"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.decidedBy").value("mike.approver"))
                    .andExpect(jsonPath("$.decidedAt").exists());
        }

        @Test
        @WithMockUser(username = "mike.approver", roles = "APPROVER")
        void rejectWithReason_returns200() throws Exception {
            mockMvc.perform(put("/api/claims/" + pendingClaim.getId() + "/decision")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status": "REJECTED", "rejectionReason": "Missing receipt"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.rejectionReason").value("Missing receipt"));
        }

        @Test
        @WithMockUser(username = "mike.approver", roles = "APPROVER")
        void rejectWithoutReason_returns400() throws Exception {
            mockMvc.perform(put("/api/claims/" + pendingClaim.getId() + "/decision")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status": "REJECTED"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "mike.approver", roles = "APPROVER")
        void decideAlreadyDecidedClaim_returns403() throws Exception {
            mockMvc.perform(put("/api/claims/" + approvedClaim.getId() + "/decision")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status": "APPROVED"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "john.smith", roles = "EMPLOYEE")
        void employeeTriesToDecide_returns403() throws Exception {
            mockMvc.perform(put("/api/claims/" + pendingClaim.getId() + "/decision")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status": "APPROVED"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "mike.approver", roles = "APPROVER")
        void decideNonExistentClaim_returns404() throws Exception {
            mockMvc.perform(put("/api/claims/99999/decision")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status": "APPROVED"}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "mike.approver", roles = "APPROVER")
        void setPendingViaDecision_returns400() throws Exception {
            mockMvc.perform(put("/api/claims/" + pendingClaim.getId() + "/decision")
                            .header(HEADER, HEADER_VALUE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status": "PENDING"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }
}

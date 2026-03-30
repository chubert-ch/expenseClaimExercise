package com.chuber.expenseclaim;

import com.chuber.expenseclaim.repository.ExpenseClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseClaimFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ExpenseClaimRepository claimRepository;

    @BeforeEach
    void setUp() {
        claimRepository.deleteAll();
    }

    @Test
    void fullExpenseClaimWorkflow() throws Exception {
        // === Login all users ===

        MockHttpSession employee1Session = login("john.smith", "Password123!");
        MockHttpSession employee2Session = login("jane.doe", "Password456!");
        MockHttpSession approverSession = login("mike.approver", "ApproverPass1!");

        // === Employee 1 submits a claim ===

        MvcResult e1ClaimResult = mockMvc.perform(post("/api/claims")
                        .with(csrf())
                        .session(employee1Session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Train to London",
                                  "amount": 85.50,
                                  "expenseDate": "2026-03-20",
                                  "category": "TRAVEL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employee").value("john.smith"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String e1ClaimId = extractId(e1ClaimResult);

        // === Employee 2 submits two claims ===

        MvcResult e2Claim1Result = mockMvc.perform(post("/api/claims")
                        .with(csrf())
                        .session(employee2Session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Client dinner",
                                  "amount": 65.00,
                                  "expenseDate": "2026-03-18",
                                  "category": "MEALS"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employee").value("jane.doe"))
                .andReturn();

        String e2Claim1Id = extractId(e2Claim1Result);

        mockMvc.perform(post("/api/claims")
                        .with(csrf())
                        .session(employee2Session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "USB keyboard",
                                  "amount": 45.99,
                                  "expenseDate": "2026-03-19",
                                  "category": "EQUIPMENT"
                                }
                                """))
                .andExpect(status().isCreated());

        // === Employee 1 views their list — should see only their 1 claim ===

        mockMvc.perform(get("/api/claims")
                        .session(employee1Session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Train to London"));

        // === Employee 1 views their specific claim ===

        mockMvc.perform(get("/api/claims/" + e1ClaimId)
                        .session(employee1Session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Train to London"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // === Approver views all claims — should see all 3 ===

        mockMvc.perform(get("/api/claims")
                        .session(approverSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // === Approver views employee 1's claim specifically ===

        mockMvc.perform(get("/api/claims/" + e1ClaimId)
                        .session(approverSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee").value("john.smith"));

        // === Approver approves employee 1's claim ===

        mockMvc.perform(put("/api/claims/" + e1ClaimId + "/decision")
                        .with(csrf())
                        .session(approverSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "APPROVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").value("mike.approver"))
                .andExpect(jsonPath("$.decidedAt").exists());

        // === Approver tries to reject employee 2's first claim without a reason — 400 ===

        mockMvc.perform(put("/api/claims/" + e2Claim1Id + "/decision")
                        .with(csrf())
                        .session(approverSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "REJECTED"}
                                """))
                .andExpect(status().isBadRequest());

        // === Approver rejects employee 2's first claim with a reason — succeeds ===

        mockMvc.perform(put("/api/claims/" + e2Claim1Id + "/decision")
                        .with(csrf())
                        .session(approverSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "REJECTED", "rejectionReason": "No receipt attached"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("No receipt attached"));

        // === Approver tries to reject the same claim again — 422 ===

        mockMvc.perform(put("/api/claims/" + e2Claim1Id + "/decision")
                        .with(csrf())
                        .session(approverSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "REJECTED", "rejectionReason": "Actually, wrong code"}
                                """))
                .andExpect(status().isForbidden());

        // === Employee 1 views their list — claim should now be APPROVED ===

        mockMvc.perform(get("/api/claims")
                        .session(employee1Session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].decidedBy").value("mike.approver"));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String extractId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.id").toString();
    }
}

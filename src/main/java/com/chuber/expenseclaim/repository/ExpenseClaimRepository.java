package com.chuber.expenseclaim.repository;

import com.chuber.expenseclaim.entity.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long> {

    List<ExpenseClaim> findByEmployeeUsername(String username);
}

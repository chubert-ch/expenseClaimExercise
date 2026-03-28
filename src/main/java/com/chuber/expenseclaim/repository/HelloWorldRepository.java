package com.chuber.expenseclaim.repository;

import com.chuber.expenseclaim.entity.HelloWorld;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HelloWorldRepository extends JpaRepository<HelloWorld, Long> {
}

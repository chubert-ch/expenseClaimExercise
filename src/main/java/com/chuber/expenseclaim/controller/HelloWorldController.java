package com.chuber.expenseclaim.controller;

import com.chuber.expenseclaim.entity.HelloWorld;
import com.chuber.expenseclaim.repository.HelloWorldRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HelloWorldController {

    private final HelloWorldRepository repository;

    public HelloWorldController(HelloWorldRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/hello")
    public List<HelloWorld> hello() {
        return repository.findAll();
    }
}

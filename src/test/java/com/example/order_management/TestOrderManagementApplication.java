package com.example.order_management;

import org.springframework.boot.SpringApplication;

public class TestOrderManagementApplication {

    public static void main(String[] args) {
        SpringApplication.from(OrderManagementApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

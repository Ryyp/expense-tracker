package com.dalrae.expensetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application for Expense Tracker
 *
 * This application provides budget management and expense tracking functionality
 * using SAP Cloud Application Programming Model (CAP) with Java.
 *
 * @author Expense Tracker Team
 * @version 1.0
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

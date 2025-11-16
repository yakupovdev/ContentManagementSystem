package com.github.yakupovdev.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ContentManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentManagementSystemApplication.class, args);

        System.out.println("""
                
                ╔═══════════════════════════════════════════════════════╗
                ║   Content Management System - Started Successfully    ║
                ║   Instagram Post Generator with GPT-4o                ║
                ║   Port: 8080                                          ║
                ║   Database: PostgreSQL                                ║
                ║   API Docs: http://localhost:8080/api                 ║
                ╚═══════════════════════════════════════════════════════╝
                
                Available Endpoints:
                ├─ POST /api/auth/register    - Register new user
                ├─ POST /api/auth/login       - Login user
                ├─ GET  /api/users/me         - Get user info
                ├─ POST /api/posts/generate   - Generate post with GPT-4o
                ├─ POST /api/posts/save       - Save generated post
                ├─ GET  /api/posts            - Get all user posts
                ├─ GET  /api/posts/{id}       - Get specific post
                └─ DELETE /api/posts/{id}     - Delete post
                
                """);
    }
}
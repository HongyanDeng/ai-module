package com.example.llm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @GetMapping("/db")
    public String testDb() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "数据库连接成功！";
        } catch (Exception e) {
            return "数据库连接失败：" + e.getMessage();
        }
    }
} 
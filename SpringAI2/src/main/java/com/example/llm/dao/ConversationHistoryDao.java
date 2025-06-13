/**package com.example.llm.dao;

import com.example.llm.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationHistoryDao extends JpaRepository<ConversationHistory, String> {
    List<ConversationHistory> findTop10BySessionIdOrderByCreateTimeDesc(String sessionId);
}*/

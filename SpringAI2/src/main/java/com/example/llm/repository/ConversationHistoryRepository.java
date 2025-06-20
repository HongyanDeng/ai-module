package com.example.llm.repository;

import com.example.llm.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, String> {

    List<ConversationHistory> findBySessionIdOrderByCreateTimeAsc(String sessionId);
}

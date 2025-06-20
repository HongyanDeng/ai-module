package com.example.llm.service;

import com.example.llm.entity.ConversationHistory;
import java.util.List;

public interface ConversationService {

    void saveConversation(ConversationHistory conversationHistory);

    List<ConversationHistory> getConversationHistoryBySessionId(String sessionId);
}

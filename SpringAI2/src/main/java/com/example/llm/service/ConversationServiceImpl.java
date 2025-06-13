package com.example.llm.service;

import com.example.llm.entity.ConversationHistory;
import com.example.llm.repository.ConversationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public List<ConversationHistory> getConversationHistory(String conversationId) {
        return conversationHistoryRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
    }

    @Override
    public List<ConversationHistory> getConversationHistoryBySessionId(String sessionId) {
        return conversationHistoryRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }

    @Override
    public void saveConversation(ConversationHistory conversationHistory) {
        conversationHistoryRepository.save(conversationHistory);
    }
} 
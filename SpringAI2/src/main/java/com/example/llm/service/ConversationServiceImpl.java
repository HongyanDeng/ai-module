package com.example.llm.service;

import com.example.llm.entity.ConversationHistory;
import com.example.llm.repository.ConversationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public List<ConversationHistory> getConversationHistoryBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("Attempt to fetch conversation history with empty session ID");
            return Collections.emptyList();
        }

        log.info("Fetching conversation history for session ID: {}", sessionId);

        try {
            List<ConversationHistory> historyList = conversationHistoryRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
            return Collections.unmodifiableList(historyList); // 返回不可变列表
        } catch (Exception e) {
            log.error("Error fetching conversation history for session ID: {}", sessionId, e);
            throw e; // 或者返回空列表：return Collections.emptyList();
        }
    }

    @Override
    public void saveConversation(ConversationHistory conversationHistory) {
        conversationHistoryRepository.save(conversationHistory);
    }

}

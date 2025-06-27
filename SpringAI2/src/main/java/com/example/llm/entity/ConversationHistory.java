package com.example.llm.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conversation_history")
@EntityListeners(AuditingEntityListener.class)
public class ConversationHistory {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "VARCHAR(255)", nullable = false)
    private String id;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "ai_conversation_id")
    private String aiConversationId;

    @Column(name = "ai_message_id")
    private String aiMessageId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "role")
    private String role;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_device")
    private String userDevice;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "model_name")
    private String modelName;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "file_id")
    private String fileId;

}

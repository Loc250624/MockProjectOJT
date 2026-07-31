package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.AiChatSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatSuggestionRepository extends JpaRepository<AiChatSuggestion, Long> {

    @Query("""
            select s.displayText
            from AiChatSuggestion s
            where s.conversation.id = :conversationId
              and s.conversation.user.id = :userId
            order by s.id asc
            """)
    List<String> findOwnedDisplayText(@Param("conversationId") String conversationId,
                                      @Param("userId") Integer userId);

    List<AiChatSuggestion> findByAssistantMessageIdOrderByIdAsc(Long assistantMessageId);
}

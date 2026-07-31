package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.AiChatMessage;
import com.ojtsu26.elearning.model.enums.AiChatMessageRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    @Query("""
            select m
            from AiChatMessage m
            where m.conversation.id = :conversationId
              and m.conversation.user.id = :userId
              and (:beforeId is null or m.id < :beforeId)
            order by m.id desc
            """)
    List<AiChatMessage> findOwnedMessagesBefore(@Param("conversationId") String conversationId,
                                                @Param("userId") Integer userId,
                                                @Param("beforeId") Long beforeId,
                                                Pageable pageable);

    @Query("""
            select m.content
            from AiChatMessage m
            where m.conversation.id = :conversationId
              and m.conversation.user.id = :userId
              and m.role = :role
            order by m.id asc
            """)
    List<String> findOwnedContentByRole(@Param("conversationId") String conversationId,
                                        @Param("userId") Integer userId,
                                        @Param("role") AiChatMessageRole role);

    Optional<AiChatMessage> findFirstByConversationIdAndConversationUserIdAndRoleOrderByIdDesc(
            String conversationId,
            Integer userId,
            AiChatMessageRole role);

    long countByConversationId(String conversationId);
}

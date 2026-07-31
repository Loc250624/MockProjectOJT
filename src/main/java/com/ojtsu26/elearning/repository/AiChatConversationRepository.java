package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.AiChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiChatConversationRepository extends JpaRepository<AiChatConversation, String> {

    Optional<AiChatConversation> findFirstByUserIdOrderByLastMessageAtDescIdDesc(Integer userId);

    Optional<AiChatConversation> findByIdAndUserId(String id, Integer userId);
}

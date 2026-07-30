package com.ojtsu26.elearning.service.ai;

import com.ojtsu26.elearning.config.AiTutorProperties;
import com.ojtsu26.elearning.dto.response.AiChatMessagePageDTO;
import com.ojtsu26.elearning.dto.response.AiTutorChatResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.AiChatConversation;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.AiChatConversationRepository;
import com.ojtsu26.elearning.repository.AiChatMessageRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai_chat_history_service;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Import({AiChatHistoryService.class, AiChatSuggestionService.class, AiTutorProperties.class})
class AiChatHistoryServiceTest {

    @Autowired
    private AiChatHistoryService historyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiChatConversationRepository conversationRepository;

    @Autowired
    private AiChatMessageRepository messageRepository;

    @Test
    void appendTurnIsTransactionalAndMessagesAreReturnedChronologically() {
        User user = user("history-a@example.com");
        AiTutorChatResponseDTO response = AiTutorChatResponseDTO.builder()
                .answer("Open My Courses from the student dashboard.")
                .requestId("request-1")
                .build();

        AiChatConversation conversation = historyService.appendTurn(
                user,
                null,
                "Where are my courses?",
                response,
                List.of("How is course progress tracked?"));

        AiChatMessagePageDTO page = historyService.messages(user, conversation.getId(), 50, null);

        assertNotNull(conversation.getLastMessageAt());
        assertEquals(2, messageRepository.countByConversationId(conversation.getId()));
        assertEquals(List.of("USER", "ASSISTANT"),
                page.getMessages().stream().map(message -> message.getRole()).toList());
        assertEquals("Where are my courses?", page.getMessages().get(0).getContent());
        assertEquals("Open My Courses from the student dashboard.", page.getMessages().get(1).getContent());
    }

    @Test
    void anotherUserCannotResolveOrReadConversation() {
        User owner = user("history-owner@example.com");
        User other = user("history-other@example.com");
        AiChatConversation conversation = historyService.appendTurn(
                owner,
                null,
                "How do I enroll?",
                AiTutorChatResponseDTO.builder().answer("Open a course detail page.").build(),
                List.of("Where can I see enrolled courses?"));

        assertThrows(BusinessException.class,
                () -> historyService.resolveOwnedConversation(other.getId(), conversation.getId()));
        assertThrows(BusinessException.class,
                () -> historyService.messages(other, conversation.getId(), 50, null));
        assertEquals(conversation.getId(),
                conversationRepository.findFirstByUserIdOrderByLastMessageAtDescIdDesc(owner.getId())
                        .orElseThrow()
                        .getId());
    }

    @Test
    void messagePaginationUsesBoundedCursorWindow() {
        User user = user("history-page@example.com");
        AiChatConversation conversation = null;
        for (int turn = 1; turn <= 3; turn++) {
            conversation = historyService.appendTurn(
                    user,
                    conversation,
                    "Question " + turn,
                    AiTutorChatResponseDTO.builder().answer("Answer " + turn).build(),
                    List.of("Related question " + turn + "?"));
        }

        AiChatMessagePageDTO newest = historyService.messages(user, conversation.getId(), 2, null);
        AiChatMessagePageDTO older = historyService.messages(
                user,
                conversation.getId(),
                2,
                newest.getNextBeforeId());

        assertEquals(2, newest.getMessages().size());
        assertEquals(true, newest.isHasMore());
        assertEquals(List.of("Question 3", "Answer 3"),
                newest.getMessages().stream().map(message -> message.getContent()).toList());
        assertEquals(List.of("Question 2", "Answer 2"),
                older.getMessages().stream().map(message -> message.getContent()).toList());
    }

    @Test
    void assistantContentLongerThanTinyTextLimitIsPersistedWithoutTruncation() {
        User user = user("history-long-content@example.com");
        String longAnswer = "Detailed chatbot guidance. ".repeat(40);

        AiChatConversation conversation = historyService.appendTurn(
                user,
                null,
                "Please provide detailed guidance.",
                AiTutorChatResponseDTO.builder().answer(longAnswer).build(),
                List.of("What should I review next?"));
        AiChatMessagePageDTO page = historyService.messages(user, conversation.getId(), 50, null);

        assertEquals(longAnswer.trim(), page.getMessages().get(1).getContent());
    }

    private User user(String email) {
        return userRepository.save(User.builder()
                .fullName(email)
                .email(email)
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build());
    }
}

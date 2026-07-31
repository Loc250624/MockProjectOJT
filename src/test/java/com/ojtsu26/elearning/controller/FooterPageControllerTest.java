package com.ojtsu26.elearning.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:footer_page_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class FooterPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allNewFooterRoutesArePubliclyAvailable() throws Exception {
        String[] routes = {
                "/about/introduction",
                "/contact",
                "/legal/terms-of-use",
                "/legal/privacy-policy",
                "/support/help-center",
                "/support/learning-guide",
                "/support/faq",
                "/support/payment-policy",
                "/certificates",
                "/ai-chatbot"
        };

        for (String route : routes) {
            mockMvc.perform(get(route))
                    .andExpect(status().isOk())
                    .andExpect(view().name("public/footer-information"));
        }
    }

    @Test
    void introductionRendersExpectedContentAndRealFooterLinks() throws Exception {
        mockMvc.perform(get("/about/introduction"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pageTitle", "About LumiNa"))
                .andExpect(model().attribute("pageSections", hasSize(3)))
                .andExpect(content().string(containsString("href=\"/about/introduction\"")))
                .andExpect(content().string(containsString("href=\"/contact\"")))
                .andExpect(content().string(containsString("href=\"/legal/terms-of-use\"")))
                .andExpect(content().string(containsString("href=\"/legal/privacy-policy\"")))
                .andExpect(content().string(containsString("href=\"/support/help-center\"")))
                .andExpect(content().string(containsString("href=\"/support/learning-guide\"")))
                .andExpect(content().string(containsString("href=\"/support/faq\"")))
                .andExpect(content().string(containsString("href=\"/support/payment-policy\"")))
                .andExpect(content().string(containsString("href=\"/courses\"")))
                .andExpect(content().string(containsString("href=\"/blogs\"")))
                .andExpect(content().string(containsString("href=\"/certificates\"")))
                .andExpect(content().string(containsString("href=\"/ai-chatbot\"")));
    }

    @Test
    void certificateRouteIsInformationOnly() throws Exception {
        mockMvc.perform(get("/certificates"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pageTitle", "Certificates"))
                .andExpect(content().string(containsString("separate from this public information page")))
                .andExpect(content().string(containsString("does not generate a certificate")));
    }

    @Test
    void aiChatbotPageReusesExistingWidgetAndProvidesOpenAction() throws Exception {
        mockMvc.perform(get("/ai-chatbot"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("openChatbotAction", true))
                .andExpect(content().string(containsString("data-open-ai-chatbot")))
                .andExpect(content().string(containsString("data-ai-chatbot-toggle")))
                .andExpect(content().string(containsString("/js/ai-chatbot.js")))
                .andExpect(content().string(containsString("/js/public/footer-information.js")));
    }
}

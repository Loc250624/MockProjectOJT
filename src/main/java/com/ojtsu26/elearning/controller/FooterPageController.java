package com.ojtsu26.elearning.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public, static informational pages linked from the shared footer.
 *
 * <p>These pages do not change authentication, payments, certificates,
 * course data, or AI behavior.</p>
 */
@Controller
public class FooterPageController {

    private static final Map<String, PageContent> PAGES = createPages();

    @GetMapping("/about/introduction")
    public String introduction(Model model) {
        return render("introduction", model);
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        return render("contact", model);
    }

    @GetMapping("/legal/terms-of-use")
    public String termsOfUse(Model model) {
        return render("terms", model);
    }

    @GetMapping("/legal/privacy-policy")
    public String privacyPolicy(Model model) {
        return render("privacy", model);
    }

    @GetMapping("/support/help-center")
    public String helpCenter(Model model) {
        return render("help", model);
    }

    @GetMapping("/support/learning-guide")
    public String learningGuide(Model model) {
        return render("learning-guide", model);
    }

    @GetMapping("/support/faq")
    public String frequentlyAskedQuestions(Model model) {
        return render("faq", model);
    }

    @GetMapping("/support/payment-policy")
    public String paymentPolicy(Model model) {
        return render("payment-policy", model);
    }

    @GetMapping("/certificates")
    public String certificates(Model model) {
        return render("certificates", model);
    }

    @GetMapping("/ai-chatbot")
    public String aiChatbot(Model model) {
        model.addAttribute("openChatbotAction", true);
        return render("ai-chatbot", model);
    }

    private String render(String pageKey, Model model) {
        PageContent page = PAGES.get(pageKey);
        if (page == null) {
            throw new IllegalArgumentException("Unknown footer page: " + pageKey);
        }

        model.addAttribute("pageTitle", page.title());
        model.addAttribute("pageDescription", page.description());
        model.addAttribute("pageEyebrow", page.eyebrow());
        model.addAttribute("pageSections", page.sections());
        model.addAttribute("pageActions", page.actions());
        return "public/footer-information";
    }

    private static Map<String, PageContent> createPages() {
        Map<String, PageContent> pages = new LinkedHashMap<>();

        pages.put("introduction", page(
                "About LumiNa",
                "Introduction",
                "LumiNa connects course discovery, structured lessons, quizzes, progress tracking, blogs, payments, AI assistance, and certificates in one learning experience.",
                List.of(
                        section("What learners can do",
                                "Browse approved courses, enroll through the supported checkout flow, study lesson content, complete quizzes, review progress, and access earned certificates."),
                        section("What teachers can do",
                                "Create and manage courses, organize lessons and question banks, publish learning content, and review student learning analytics within the permissions of the teacher role."),
                        section("How access works",
                                "Public pages are available to visitors. Student, Teacher, and Admin workspaces remain protected by role-aware authentication and authorization.")
                ),
                List.of(action("Browse courses", "/courses"), action("Read the blog", "/blogs"))
        ));

        pages.put("contact", page(
                "Contact and Support",
                "Contact",
                "Use the support path that matches your issue so the relevant account, course, payment, or certificate context is available.",
                List.of(
                        section("Learning and course questions",
                                "Open the Help Center first, then include the course name, lesson name, and a short description of the issue when submitting feedback from your student account."),
                        section("Account access",
                                "For login or profile issues, record the sign-in method used, the approximate time of the problem, and the exact error message. Never include passwords or OAuth secrets."),
                        section("Payment and certificate issues",
                                "Keep the transaction reference or certificate verification code available. Do not publish sensitive payment details in blog comments or public areas.")
                ),
                List.of(action("Open Help Center", "/support/help-center"), action("View FAQ", "/support/faq"))
        ));

        pages.put("terms", page(
                "Terms of Use",
                "Legal",
                "These product terms describe responsible use of LumiNa. They should be reviewed by the project owner before being treated as final legal terms.",
                List.of(
                        section("Account responsibility",
                                "Users are responsible for keeping their sign-in credentials secure and for activity performed through their account. Shared or unauthorized account use is not permitted."),
                        section("Learning content",
                                "Course, lesson, quiz, blog, and certificate content may only be used for authorized learning purposes. Users must respect intellectual property and must not attempt to bypass role restrictions."),
                        section("Acceptable use",
                                "Do not disrupt the platform, probe protected endpoints without authorization, submit malicious files, manipulate quiz or payment records, or misuse the AI Chatbot."),
                        section("Service changes",
                                "Features, course availability, payment providers, and policies may change as the platform is maintained. Material policy updates should be published with an effective date.")
                ),
                List.of(action("Privacy Policy", "/legal/privacy-policy"), action("Help Center", "/support/help-center"))
        ));

        pages.put("privacy", page(
                "Privacy Policy",
                "Legal",
                "This page explains the categories of information LumiNa may process to provide authentication, learning, payment, support, and certificate features.",
                List.of(
                        section("Information used by the platform",
                                "Depending on the feature, LumiNa may process account profile data, role information, enrollments, lesson progress, quiz activity, feedback, payment status, blog activity, and certificate records."),
                        section("Why information is processed",
                                "Information is used to authenticate users, enforce permissions, deliver course features, preserve progress, process supported payments, issue certificates, respond to support requests, and maintain platform security."),
                        section("Security and access",
                                "Protected information should only be accessible to authorized users and services. Credentials, tokens, and payment secrets must not be exposed in frontend templates, logs, public repositories, or support messages."),
                        section("User choices",
                                "Users should be able to review and update appropriate profile information. Requests involving deletion, retention, or legal rights must follow the process defined by the project owner and applicable requirements.")
                ),
                List.of(action("Terms of Use", "/legal/terms-of-use"), action("Contact", "/contact"))
        ));

        pages.put("help", page(
                "Help Center",
                "Support",
                "Find the correct LumiNa workflow before reporting a problem.",
                List.of(
                        section("Courses and enrollment",
                                "Use the public course catalog to view approved courses. Sign in as a Student to enroll, access purchased or free courses, and continue learning."),
                        section("Lessons and quizzes",
                                "Open My Courses, choose a course, and continue from the learning page. Quiz answers should be saved or submitted only through the official quiz flow."),
                        section("Payments",
                                "Complete checkout through the payment page and wait for the official result screen. Avoid refreshing, duplicating, or manually editing callback URLs during a transaction."),
                        section("Certificates",
                                "Certificates appear after the platform confirms eligibility. Use the certificate verification code on the verification page when validating an issued certificate."),
                        section("AI Chatbot",
                                "The AI Chatbot can explain LumiNa navigation and authorized lesson context. It does not replace official grades, payment status, or certificate eligibility rules.")
                ),
                List.of(action("Learning Guide", "/support/learning-guide"), action("Frequently Asked Questions", "/support/faq"))
        ));

        pages.put("learning-guide", page(
                "Learning Guide",
                "Support",
                "A practical path from course discovery to completion.",
                List.of(
                        section("1. Discover",
                                "Browse the course catalog, filter available courses, and open the detail page to review the description, instructor, price, and lesson structure."),
                        section("2. Enroll",
                                "Sign in as a Student and follow the official enrollment or checkout flow. Confirm the result only from LumiNa's payment result page."),
                        section("3. Learn",
                                "Open My Courses, choose a course, and work through lessons in the intended order. Progress is saved through the application's learning flow."),
                        section("4. Practice and review",
                                "Complete lesson quizzes, review submitted results, and revisit course content when additional practice is needed."),
                        section("5. Complete and verify",
                                "After all eligibility conditions are satisfied, access the certificate area and use the provided verification code when independent verification is required.")
                ),
                List.of(action("Browse courses", "/courses"), action("Certificates", "/certificates"))
        ));

        pages.put("faq", page(
                "Frequently Asked Questions",
                "Support",
                "Common answers for visitors and learners using LumiNa.",
                List.of(
                        section("Do I need an account to browse courses?",
                                "No. Approved courses and published blogs can be viewed from the public area. Enrollment and protected learning features require the correct account role."),
                        section("Where are my enrolled courses?",
                                "Sign in as a Student and open My Courses from the student navigation."),
                        section("Can I review a completed course?",
                                "Completed courses remain available for review when the course and account are still accessible under the platform rules."),
                        section("Why is a certificate not available yet?",
                                "Certificate availability depends on the platform's eligibility checks, such as required lesson or course completion conditions."),
                        section("Can the AI Chatbot change my grade or payment?",
                                "No. The AI Chatbot provides guidance and explanations; it does not directly change quiz grades, enrollment state, payments, progress, or certificate eligibility.")
                ),
                List.of(action("Help Center", "/support/help-center"), action("AI Chatbot", "/ai-chatbot"))
        ));

        pages.put("payment-policy", page(
                "Payment Policy",
                "Support",
                "This page describes safe use of LumiNa's supported checkout flow. Final commercial and refund terms must be approved by the project owner.",
                List.of(
                        section("Official checkout flow",
                                "Start payment from the selected course's official checkout page. The final status must come from LumiNa's validated payment result or transaction history."),
                        section("Transaction status",
                                "A pending, failed, cancelled, or successful result must be handled according to the status returned by the supported payment integration. Do not treat a browser redirect alone as proof of payment."),
                        section("Duplicate or interrupted payments",
                                "Before retrying, review payment history and avoid creating repeated transactions while a prior payment is still being processed."),
                        section("Refund review",
                                "Refund eligibility is not automatic. It should be reviewed using the transaction state, course access state, applicable project rules, and the supported payment provider's constraints."),
                        section("Protecting payment information",
                                "Never place gateway secrets, card credentials, passwords, access tokens, or full sensitive transaction data in public comments, screenshots, or source control.")
                ),
                List.of(action("Help Center", "/support/help-center"), action("Browse courses", "/courses"))
        ));

        pages.put("certificates", page(
                "Certificates",
                "Resources",
                "LumiNa certificates document course completion after the platform confirms the required eligibility conditions.",
                List.of(
                        section("Earning a certificate",
                                "A certificate is generated only after the relevant course completion rules are satisfied. The certificate area for a signed-in Student is separate from this public information page."),
                        section("Viewing earned certificates",
                                "Sign in as a Student and open the Certificates section to view or download certificates associated with the account."),
                        section("Verification",
                                "Issued certificates contain a verification code. The code can be used with LumiNa's certificate verification route to confirm the stored certificate record."),
                        section("Important limitation",
                                "This public page does not generate a certificate, change completion status, or bypass eligibility checks.")
                ),
                List.of(action("Browse courses", "/courses"), action("Learning Guide", "/support/learning-guide"))
        ));

        pages.put("ai-chatbot", page(
                "AI Chatbot",
                "Resources",
                "Use LumiNa's existing AI Chatbot for website guidance and authorized learning support.",
                List.of(
                        section("What it can help with",
                                "The chatbot can explain how to navigate LumiNa, clarify supported features, and assist with authorized lesson context available to the signed-in user."),
                        section("What it cannot do",
                                "It cannot guarantee factual correctness, replace teacher instructions, modify grades, submit quizzes, complete payments, change progress, or issue certificates."),
                        section("Using it safely",
                                "Do not enter passwords, API keys, payment secrets, private tokens, or other sensitive information. Verify important academic or payment decisions using the official application pages.")
                ),
                List.of(action("Browse courses", "/courses"), action("Help Center", "/support/help-center"))
        ));

        return Map.copyOf(pages);
    }

    private static PageContent page(String title,
                                    String eyebrow,
                                    String description,
                                    List<SectionContent> sections,
                                    List<ActionLink> actions) {
        return new PageContent(title, eyebrow, description, sections, actions);
    }

    private static SectionContent section(String heading, String text) {
        return new SectionContent(heading, text);
    }

    private static ActionLink action(String label, String href) {
        return new ActionLink(label, href);
    }

    public record PageContent(String title,
                              String eyebrow,
                              String description,
                              List<SectionContent> sections,
                              List<ActionLink> actions) {
    }

    public record SectionContent(String heading, String text) {
    }

    public record ActionLink(String label, String href) {
    }
}

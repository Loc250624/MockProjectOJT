package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.PendingOAuthRegistration;
import com.ojtsu26.elearning.security.OAuth2AccountService;
import com.ojtsu26.elearning.security.OAuth2ProviderConfigService;
import com.ojtsu26.elearning.security.OAuth2ProviderConfigurationFilter;
import com.ojtsu26.elearning.security.OAuth2LoginSuccessHandler;
import com.ojtsu26.elearning.service.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuth2ProviderConfigService oAuth2ProviderConfigService;
    private final OAuth2AccountService oAuth2AccountService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String errorCode, Model model) {
        boolean googleConfigured = oAuth2ProviderConfigService.isConfigured("google");
        boolean githubConfigured = oAuth2ProviderConfigService.isConfigured("github");
        model.addAttribute("googleOAuthConfigured", googleConfigured);
        model.addAttribute("githubOAuthConfigured", githubConfigured);
        model.addAttribute("oauthAnyProviderConfigured", googleConfigured || githubConfigured);

        String errorMessage = loginErrorMessage(errorCode);
        if (errorMessage != null) {
            model.addAttribute("error", errorMessage);
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(Model model) {
        model.addAttribute("message", "If this is a local LumiNa account, an administrator can generate a secure reset link.");
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam("token") String token,
                                      @RequestParam("password") String password,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      Model model) {
        try {
            passwordResetService.resetPassword(token, password, confirmPassword);
            return "redirect:/auth/login?reset=success";
        } catch (BusinessException ex) {
            model.addAttribute("token", token);
            model.addAttribute("error", ex.getMessage());
            return "auth/reset-password";
        }
    }

    @GetMapping("/oauth2/complete")
    public String completeOAuthRegistrationPage(HttpSession session, Model model) {
        String token = pendingToken(session);
        if (token == null) {
            return "redirect:/auth/login?error=oauth2";
        }

        PendingOAuthRegistration pending;
        try {
            pending = oAuth2AccountService.requireActivePending(token);
        } catch (BusinessException e) {
            session.removeAttribute(OAuth2LoginSuccessHandler.PENDING_OAUTH_SESSION_ATTRIBUTE);
            return "redirect:/auth/login?error=oauth2_pending_expired";
        }
        model.addAttribute("provider", pending.getProvider().name());
        model.addAttribute("email", pending.getEmail());
        model.addAttribute("fullName", pending.getFullName());
        model.addAttribute("avatarUrl", pending.getAvatarUrl());
        return "auth/oauth2-complete";
    }

    private String loginErrorMessage(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return null;
        }

        return switch (errorCode) {
            case "blocked" -> "This account cannot sign in. Please contact support.";
            case "deleted" -> "This account is no longer available.";
            case "role" -> "This account role is not supported. Please contact support.";
            case "oauth2_no_email" -> "OAuth sign-in did not return a usable email address.";
            case "oauth2_link_required" -> "An account already uses this email. Sign in first, then link this provider from your profile.";
            case "oauth2_pending_expired" -> "OAuth registration has expired. Please sign in again.";
            case "oauth2_retry" -> "OAuth sign-in is already being completed. Please try again.";
            case "oauth2_cancelled" -> "OAuth sign-in was cancelled.";
            case OAuth2ProviderConfigurationFilter.ERROR_PROVIDER_UNCONFIGURED ->
                    "This sign-in provider is not configured yet. Please use email/password or contact support.";
            case "oauth2" -> "OAuth sign-in could not be completed. Please try again.";
            default -> "Sign-in failed. Please check your credentials and try again.";
        };
    }

    private String pendingToken(HttpSession session) {
        Object value = session.getAttribute(OAuth2LoginSuccessHandler.PENDING_OAUTH_SESSION_ATTRIBUTE);
        return value instanceof String token && !token.isBlank() ? token : null;
    }
}

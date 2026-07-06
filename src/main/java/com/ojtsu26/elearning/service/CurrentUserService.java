package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            Integer userId = customUserDetails.getUser().getId();
            return userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}

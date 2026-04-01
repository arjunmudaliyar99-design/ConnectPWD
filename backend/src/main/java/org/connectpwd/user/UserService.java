package org.connectpwd.user;

import lombok.RequiredArgsConstructor;
import org.connectpwd.common.AppException;
import org.connectpwd.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(ErrorCode.USER_NOT_FOUND, "User not found"));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound(ErrorCode.USER_NOT_FOUND, "User not found"));
    }
}

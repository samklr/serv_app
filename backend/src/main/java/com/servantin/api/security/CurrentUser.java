package com.servantin.api.security;

import lombok.Getter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Custom annotation to get the current authenticated user ID
 */
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "@currentUserService.getCurrentUserId()")
public @interface CurrentUser {
}

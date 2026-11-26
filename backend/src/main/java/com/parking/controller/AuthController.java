package com.parking.controller;

import java.net.URI;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.parking.dto.AuthDtos.UserResponse;
import com.parking.dto.AuthDtos.loginRequest;
import com.parking.dto.AuthDtos.registerRequest;
import com.parking.dto.BanDtos.AppealRequest;
import com.parking.dto.BanDtos.BanInfo;
import com.parking.dto.BanDtos.BanStatusRequest;
import com.parking.entity.Role;
import com.parking.entity.User;
import com.parking.service.AccountBannedException;
import com.parking.service.BanService;
import com.parking.service.StaffPermissions;
import com.parking.service.UserService;
import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/auth")
@RequiredArgsConstructor 
public class AuthController {
    private final UserService userService;
    private final BanService banService;
    private final StaffPermissions staffPermissions;
    private final SecurityContextRepository securityContextRepository;

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping ("/login")
    public UserResponse login(@RequestBody loginRequest loginRequest,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        return signIn(userService.login(loginRequest), request, response);
    }

    private UserResponse signIn(User user, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = staffPermissions.authenticate(user, null, user.getRole());
        saveSession(authentication, request, response);
        return staffPermissions.response(user, authentication);
    }

    private void saveSession(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            request.changeSessionId();
        } else {
            request.getSession(true);
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /** A banned user can't log in: answer 403 with the ban reason and appeal, for the app's suspended screen. */
    @ExceptionHandler(AccountBannedException.class)
    public ResponseEntity<BanInfo> banned(AccountBannedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getBanInfo());
    }

    @PostMapping("/ban-status")
    public BanInfo banStatus(@RequestBody BanStatusRequest request) {
        return banService.status(request);
    }

    @PostMapping("/appeal")
    @ResponseStatus(HttpStatus.CREATED)
    public BanInfo appeal(@RequestBody AppealRequest request) {
        return banService.appeal(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        User user = userService.getAccount((Integer) authentication.getPrincipal());
        if (authentication.getCredentials() != null || user.isBlocked()) {
            return staffPermissions.response(user, authentication);
        }
        Authentication refreshed = staffPermissions.authenticate(user, null, user.getRole());
        if (!refreshed.getAuthorities().equals(authentication.getAuthorities())) {
            saveSession(refreshed, request, response);
        }
        return staffPermissions.response(user, refreshed);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
    @PostMapping ("/register")
    public ResponseEntity<UserResponse> register(@RequestBody registerRequest request,
                                                 HttpServletRequest servletRequest,
                                                 HttpServletResponse servletResponse) {
        return registerAccount(request, Role.USER, servletRequest, servletResponse);
    }

    @PostMapping("/staff-register")
    public ResponseEntity<UserResponse> registerStaffApplicant(@RequestBody registerRequest request,
                                                                HttpServletRequest servletRequest,
                                                                HttpServletResponse servletResponse) {
        return registerAccount(request, Role.EMPLOYEE, servletRequest, servletResponse);
    }

    private ResponseEntity<UserResponse> registerAccount(registerRequest request, Role role,
                                                         HttpServletRequest servletRequest,
                                                         HttpServletResponse servletResponse) {
        UserResponse user = signIn(userService.register(request, role), servletRequest, servletResponse);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .replacePath("/api/auth/me")
                    .build()
                    .toUri();
        return ResponseEntity.created(location).body(user);
    }
    
}

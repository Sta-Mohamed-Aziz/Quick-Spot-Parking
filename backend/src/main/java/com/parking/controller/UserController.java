package com.parking.controller;

import com.parking.dto.AuthDtos.UpdateUserRequest;
import com.parking.dto.AuthDtos.UserResponse;
import com.parking.entity.User;
import com.parking.service.UserService;
import com.parking.service.StaffPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor 
public class UserController {

    private final UserService userService;
    private final StaffPermissions staffPermissions;

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable int id, Authentication authentication) {
        requireOwnAccountOrAdmin(id, authentication);
        return responseFor(userService.getAccount(id), authentication);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable int id, @RequestBody UpdateUserRequest request,
                                   Authentication authentication) {
        requireOwnAccountOrAdmin(id, authentication);
        return responseFor(userService.updateUser(id, request), authentication);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    private UserResponse responseFor(User user, Authentication authentication) {
        return user.getId().equals(authentication.getPrincipal())
                ? staffPermissions.response(user, authentication)
                : UserResponse.from(user);
    }

    private void requireOwnAccountOrAdmin(int id, Authentication authentication) {
        boolean isAdmin = StaffPermissions.hasRole(authentication, "PLATFORM_ADMIN");
        if (!isAdmin && !Integer.valueOf(id).equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own account");
        }
    }
}

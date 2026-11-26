package com.parking.dto;

import com.parking.entity.Role;
import com.parking.entity.User;

public final class AuthDtos {
    private AuthDtos(){}
    public record loginRequest(String email, String password){}
    public record registerRequest(String name, String email, String password){}
    public record UpdateUserRequest(String name , String email , Boolean isHandicapped , Boolean isEvOwner){}
    public record UserResponse(Integer id , String name, String email , String role , boolean isHandicapped , boolean isEvOwner , boolean isBlocked, boolean platformAdmin){
        public static UserResponse from(User user){
            return from(user, user.getRole(), user.getRole() == Role.ADMIN);
        }

        public static UserResponse from(User user, Role role, boolean platformAdmin) {
            return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                role != null ? role.name() : Role.USER.name(),
                user.isHandicapped(),
                user.isEvOwner(),
                user.isBlocked(),
                platformAdmin
            );
        }
    }

    
}

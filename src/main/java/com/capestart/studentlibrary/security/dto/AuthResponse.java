package com.capestart.studentlibrary.security.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String  token;
    private String  email;
    private String  fullName;
    private String  role;
    private long    expiresIn;
}

package com.upiiz.platform_api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String emailInst;   // @ipn.mx o @alumno.ipn.mx
    private String fullName;
    private String password;
    private String role;        // Si se omite: ALUMNO para @alumno, PROFESOR para @ipn.
}

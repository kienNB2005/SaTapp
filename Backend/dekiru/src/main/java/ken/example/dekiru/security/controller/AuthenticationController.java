package ken.example.dekiru.security.controller;

import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.security.dto.LoginRequest;
import ken.example.dekiru.security.dto.RefreshTokeRequest;
import ken.example.dekiru.security.dto.LoginResponse;
import ken.example.dekiru.security.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")

    public ApiResponse<LoginResponse> login (@RequestBody LoginRequest loginRequest) throws GeneralSecurityException, IOException {
        return authenticationService.login(loginRequest);
    }

    @PostMapping("/logout")
    public void logout (@RequestBody RefreshTokeRequest refreshToken) throws ParseException {
        authenticationService.logout( refreshToken);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh (@RequestBody RefreshTokeRequest refreshToken) throws ParseException {
        return authenticationService.refresh(refreshToken);
    }

}

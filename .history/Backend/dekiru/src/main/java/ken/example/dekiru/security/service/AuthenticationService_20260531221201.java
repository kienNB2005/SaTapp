package ken.example.dekiru.security.service;
import ken.example.dekiru.security.entity.User;
import ken.example.dekiru.security.entity.RedisToken;
import ken.example.dekiru.security.entity.RefreshToken;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import ken.example.dekiru.common.response.ApiResponse;
import ken.example.dekiru.security.jwt.JwtInfo;
import ken.example.dekiru.security.jwt.TokenPayload;
import ken.example.dekiru.security.dto.LoginRequest;
import ken.example.dekiru.security.dto.RefreshTokeRequest;
import ken.example.dekiru.security.dto.LoginResponse;

import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import ken.example.dekiru.security.mapper.UserMapper;
import ken.example.dekiru.security.repository.RedisTokenRepository;
import ken.example.dekiru.security.repository.RefreshTokenRepository;
import ken.example.dekiru.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final JWTService jwtService;
    private final RedisTokenRepository redisTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    @Value("${google.client-id-web}")
    private String webId;

    @Value("${google.client-id-android}")
    private String androidId;

    @Value("${google.client-id-ios}")
    private String iosId;
    public ApiResponse<LoginResponse> login (LoginRequest loginRequest) throws GeneralSecurityException, IOException {

// 1. Verify ID Token với Google
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Arrays.asList(webId, androidId, iosId))
                .build();

        // SỬA: Phải truyền idToken gửi từ client lên (giả sử field trong DTO là idToken)
        GoogleIdToken idToken = verifier.verify(loginRequest.getIdToken());

        if (idToken == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATION); // Dùng ErrorCode của mày cho đồng bộ
        }

        // 2. Lấy thông tin "xịn" từ Google trả về
        GoogleIdToken.Payload payload = idToken.getPayload();
        String emailFromGoogle = payload.getEmail();
        String googleIdFromGoogle = payload.getSubject();

        User user = userRepository.findByGoogleId(googleIdFromGoogle)
                .orElseGet(() -> {
                    // B2: Nếu không thấy Google ID, mới tìm theo Email để thực hiện "Binding" (liên kết)
                    User userByEmail = userRepository.findByEmail(emailFromGoogle)
                            .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

                    // Kiểm tra xem tài khoản email này đã bị link với một Google ID khác chưa
                    if (userByEmail.getGoogleId() != null && !userByEmail.getGoogleId().equals(googleIdFromGoogle)) {
                        throw new RuntimeException("Tài khoản này đã liên kết với một tài khoản Google khác!");
                    }

                    // Nếu tài khoản hợp lệ và chưa có Google ID -> Cập nhật (Link tài khoản)
                    userByEmail.setGoogleId(googleIdFromGoogle);
                    return userRepository.save(userByEmail);
                });

        // B2: Kiểm tra trạng thái hoạt động
        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản đã bị khóa!");
        }

//        // B3: Cập nhật googleId nếu là lần đầu (Binding tài khoản)
//        if (user.getGoogleId() == null) {
//            user.setGoogleId(googleIdFromGoogle);
//            userRepository.save(user);
//        } else if (!user.getGoogleId().equals(googleIdFromGoogle)) {
//            throw new RuntimeException("Tài khoản Google này không khớp với dữ liệu đăng ký!");
//        }

        TokenPayload accessPayload = jwtService.generateAccessToken(user);

        TokenPayload refreshPayload = jwtService.generateRefreshToken(user);
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .tokenId(refreshPayload.getJwtId())
                        .userId(user.getId())
                        .expiryDate(refreshPayload.getExpiredTime())
                        .build()
        );

        return  ApiResponse.<LoginResponse>builder()
                .result(LoginResponse.builder()
                        .accessToken(accessPayload.getToken())
                        .refreshToken(refreshPayload.getToken())
                        .build())
                .build();
    }

    // Khi đăng xuất thì sẽ lưu những token còn hạn vào blacklist redis
    public void logout (RefreshTokeRequest refreshToken) throws ParseException {
        JwtInfo info = jwtService.parseToken(refreshToken.getRefreshToken());

        refreshTokenRepository.deleteById(info.getJwtId());

//        JwtInfo jwtInfo = jwtService.parseToken(token);
//
//        String jwtId = jwtInfo.getJwtId();
//        Date expiredTime = jwtInfo.getExpiredTime();
//
//        // nếu token đã hết hạn thì khỏi blacklist
//        if (expiredTime.before(new Date())) {
//            return;
//        }
//
//        long ttl = (expiredTime.getTime() - System.currentTimeMillis()) / 1000;
//
//        // tránh TTL âm
//        ttl = Math.max(ttl, 0);
//
//        RedisToken redisToken = RedisToken.builder()
//                .jwtId(jwtId)
//                .ttl(ttl)
//                .build();
//
//        redisTokenRepository.save(redisToken);
    }

    public ApiResponse<LoginResponse> refresh(RefreshTokeRequest request) throws ParseException {

        JwtInfo info = jwtService.parseToken(request.getRefreshToken());

        RefreshToken oldToken = refreshTokenRepository.findById(info.getJwtId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        // check expired
        if (oldToken.getExpiryDate().before(new Date())) {
            refreshTokenRepository.deleteById(oldToken.getTokenId());
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        // ❗ xóa token cũ (QUAN TRỌNG)
        refreshTokenRepository.deleteById(oldToken.getTokenId());

        // generate mới
        TokenPayload newAccess = jwtService.generateAccessToken(user);
        TokenPayload newRefresh = jwtService.generateRefreshToken(user);

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .tokenId(newRefresh.getJwtId())
                        .userId(user.getId())
                        .expiryDate(newRefresh.getExpiredTime())
                        .build()
        );

        return ApiResponse.<LoginResponse>builder()
                .result(LoginResponse.builder()
                        .accessToken(newAccess.getToken())
                        .refreshToken(newRefresh.getToken())
                        .build())
                .build();
    }
}

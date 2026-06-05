package ken.example.dekiru.security.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import ken.example.dekiru.common.exception.AppException;
import ken.example.dekiru.common.exception.ErrorCode;
import ken.example.dekiru.security.jwt.JwtInfo;
import ken.example.dekiru.security.jwt.TokenPayload;
import ken.example.dekiru.security.entity.User;
import ken.example.dekiru.academic.repository.LecturerRepository;
import ken.example.dekiru.security.repository.RedisTokenRepository;
import ken.example.dekiru.student.repository.StudentRepository;
import ken.example.dekiru.attendance.dto.QrDataPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JWTService {

    private final RedisTokenRepository redisTokenRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    @Value("${jwt.secret-key}")
    private String secretKey;


    // ===== CORE GENERATOR =====
    private TokenPayload generateToken(User user, long amount, ChronoUnit unit) {

        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

            Date issueTime = new Date();
            Date expiredTime = Date.from(issueTime.toInstant().plus(amount, unit));

            String jwtId = UUID.randomUUID().toString();

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(user.getEmail())
                    .jwtID(jwtId)
                    .issueTime(issueTime)
                    .expirationTime(expiredTime)
                    // Chuẩn hóa Role cho Spring Security (VD: ROLE_LECTURER, ROLE_ADMIN)
                    .claim("roles", "ROLE_" + String.valueOf(user.getRole()).toUpperCase())
                    // Luôn luôn có userId gốc
                    .claim("userId", user.getId());

            // 2. Kiểm tra Role và nhét ID tương ứng một cách an toàn
            String roleName = String.valueOf(user.getRole()).toUpperCase();

            if ("LECTURER".equals(roleName)) {
                lecturerRepository.findByUserId(user.getId())
                        .ifPresent(lecturer -> claimsBuilder.claim("lecturerId", lecturer.getId()));
            }
            else if ("STUDENT".equals(roleName)) {
                studentRepository.findByUserId(user.getId())
                        .ifPresent(student -> claimsBuilder.claim("studentId", student.getId()));
            }

            // 3. Đóng gói Claims
            SignedJWT jwt = new SignedJWT(header, claimsBuilder.build());
            jwt.sign(new MACSigner(secretKey));

            return TokenPayload.builder()
                    .jwtId(jwtId)
                    .token(jwt.serialize())
                    .expiredTime(expiredTime)
                    .build();

        } catch (JOSEException e) {
            throw new AppException(ErrorCode.CAN_NOT_GENERATE_TOKEN);
        }
    }
    // ===== ACCESS TOKEN =====
    public TokenPayload generateAccessToken(User user) {

        return generateToken(user, 30, ChronoUnit.MINUTES);
    }

    // ===== REFRESH TOKEN =====
    public TokenPayload generateRefreshToken(User user) {

        return generateToken(user, 30, ChronoUnit.DAYS);
    }



    // decoder
    // ===== VERIFY =====
    public boolean verifyToken(String token) throws ParseException, JOSEException {
        // Kiểm tra xem token có rỗng không trước khi parse
        if (token == null || token.isBlank()) {
            return false;
        }
            SignedJWT jwt = SignedJWT.parse(token);

            Date exp = jwt.getJWTClaimsSet().getExpirationTime();

            if (exp.before(new Date())) {
                return false;
            }

            return jwt.verify(new MACVerifier(secretKey));

    }

    public JwtInfo parseToken (String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        return JwtInfo.builder()
                .jwtId(signedJWT.getJWTClaimsSet().getJWTID())
                .issueTime(signedJWT.getJWTClaimsSet().getIssueTime())
                .expiredTime(signedJWT.getJWTClaimsSet().getExpirationTime())
                .build();
    }

    // ===== QR TOKEN =====
    public String generateQrCodeData(Long sessionId, String qrToken, String type) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

            Date issueTime = new Date();
            Date expiredTime = Date.from(issueTime.toInstant().plus(65, ChronoUnit.SECONDS));

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject("attendance_qr")
                    .issueTime(issueTime)
                    .expirationTime(expiredTime)
                    .claim("sessionId", sessionId)
                    .claim("qrToken", qrToken)
                    .claim("type", type)
                    .build();

            SignedJWT jwt = new SignedJWT(header, claimsSet);
            jwt.sign(new MACSigner(secretKey));

            return jwt.serialize();
        } catch (JOSEException e) {
            throw new AppException(ErrorCode.CAN_NOT_GENERATE_TOKEN);
        }
    }

    public QrDataPayload extractQrCodeData(String token) {
        try {
            if (!verifyToken(token)) {
                throw new AppException(ErrorCode.QR_INVALID);
            }
            SignedJWT jwt = SignedJWT.parse(token);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            if (!"attendance_qr".equals(claims.getSubject())) {
                throw new AppException(ErrorCode.QR_INVALID);
            }

            Long sessionId = claims.getLongClaim("sessionId");
            String qrToken = claims.getStringClaim("qrToken");
            String type = claims.getStringClaim("type");

            return new QrDataPayload(sessionId, qrToken, type);
        } catch (Exception e) {
            throw new AppException(ErrorCode.QR_INVALID);
        }
    }
}

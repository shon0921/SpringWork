package kopo.poly.dto;

import kopo.poly.util.EncryptUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@ToString
@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String userId;
    private String email;
    private String mobile;
    private String provider;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String userId,
                           String email, String mobile, String provider) { // 🚨 1. 여기에 provider 파라미터 추가
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.userId = userId;
        this.email = email;
        this.mobile = mobile;
        this.provider = provider; // 🚨 2. this.provider에 파라미터로 받은 provider를 할당
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            return ofNaver("id", attributes);
        }
        return null;
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuthAttributes.builder()
                .userId((String) response.get("email"))
                .email((String) response.get("email"))
                .mobile((String) response.get("mobile"))
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .provider("naver") // "naver" 값을 빌더에 전달
                .build();
    }

    /**
     * UserDTO 엔티티 생성
     */
    public UserDTO toEntity() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return UserDTO.builder()
                .userId(email)
                .phoneNumber(mobile)
                // 만약 UserDTO에 mobile이 없다면 추가해주세요.
                .password(EncryptUtil.encHashSHA256(UUID.randomUUID().toString()))
                .regDt(now)
                .chgDt(now)
                .adminYn("n")
                .provider(provider)
                .build();
    }
}
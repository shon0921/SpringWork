package kopo.poly.service.impl;

import kopo.poly.dto.OAuthAttributes;
import kopo.poly.dto.UserDTO;
import kopo.poly.persistance.mongodb.IUserMapper;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;

@Component
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final IUserMapper userMapper;
    private final HttpSession httpSession;

    @SneakyThrows
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        UserDTO userDTO = saveOrUpdate(attributes);

        httpSession.setAttribute("userId", userDTO.getUserId());
        httpSession.setAttribute("regDt", userDTO.getRegDt());
        httpSession.setAttribute("adminYn", userDTO.getAdminYn());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes.getAttributes(),
                attributes.getNameAttributeKey());
    }

    private UserDTO saveOrUpdate(OAuthAttributes attributes) throws Exception {
        UserDTO pDTO = UserDTO.builder().userId(attributes.getUserId()).build();
        UserDTO userDTO = userMapper.getUserById(pDTO);

        String colNM = "USER";

        if (userDTO == null) { // 신규 사용자
            userDTO = attributes.toEntity();
            try {
                // 전화번호 암호화
                userDTO.setPhoneNumber(EncryptUtil.encAES128CBC(userDTO.getPhoneNumber()));
            } catch (Exception e) {
                // 예외 처리
            }
            userMapper.insertData(userDTO, colNM);

        } else { // 기존 사용자 - 전화번호 정보만 업데이트
            try {
                userDTO.setPhoneNumber(EncryptUtil.encAES128CBC(attributes.getMobile()));
            } catch (Exception e) {
                // 예외 처리
            }
            userMapper.updateUserInfo(userDTO);
        }

        return userDTO;
    }
}

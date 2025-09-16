package kopo.poly.service;

import kopo.poly.dto.UserDTO;

public interface IUserService {

    // 회원 가입 중복 확인
    int register(UserDTO pDTO) throws Exception;
    /*
        회원가입하기
     */
    int register2(UserDTO pDTO) throws Exception;

    // 로그인
    UserDTO login(UserDTO pDTO) throws Exception;

    // 이메일
    int PhoneExists(UserDTO pDTO) throws Exception;

    // 비밀번호 찾기 1
    int forgotNumber1(UserDTO pDTO) throws Exception;

    // 비밀번호 찾기2
    int forgotNumber2(UserDTO pDTO) throws Exception;

    // 비밀번호 찾기3 교체
    int forgotNumber3(UserDTO pDTO) throws Exception;

    // 회원탈퇴
    int accountDelete(UserDTO pDTO) throws Exception;

    // 기본 회원 비밀번호 확인
    int checkCurrentPassword(UserDTO pDTO) throws Exception;

    // [추가] 알림 발송을 위해 사용자 정보를 조회하고 전화번호를 복호화하는 서비스 메서드
    UserDTO getUserInfoForNotification(UserDTO pDTO) throws Exception;


}

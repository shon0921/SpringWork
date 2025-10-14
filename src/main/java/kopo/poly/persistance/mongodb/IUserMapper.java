package kopo.poly.persistance.mongodb;


import kopo.poly.dto.UserDTO;

import java.math.BigDecimal;
import java.util.Optional;

public interface IUserMapper {

    void saveSocialUser(UserDTO pDTO, String colNm) throws Exception;
    // 회원가입
    int CheckData(UserDTO pDTO, String colNm) throws Exception;

    int insertData(UserDTO pDTO, String colNm) throws Exception;

    // 로그인
    UserDTO GetLogin(UserDTO pDTO, String colNm) throws Exception;

    // 비밀번호 찾기
    int forgotPassword1(UserDTO pDTO, String colNm) throws Exception;

    int forgotPassword2(UserDTO pDTO, String colNm) throws Exception;

    // 회원탈퇴
    int deleteData(UserDTO pDTO) throws Exception;

    // 기존 회원 비밀번호 변경 전 기본 비밀번호 일치 확인
    int checkCurrentPassword(UserDTO pDTO, String colNm) throws Exception;

    // [추가] userId로 사용자 정보를 DB에서 조회하는 메서드
    UserDTO getUserInfo(UserDTO pDTO, String colNm) throws Exception;

    void updateUserInfo(UserDTO pDTO);

    UserDTO getUserById(UserDTO pDTO);

    /**
     * 누적 결제 금액 업데이트
     */
    void updateTotalAmount(String userId, BigDecimal paidAmount) throws Exception;

}
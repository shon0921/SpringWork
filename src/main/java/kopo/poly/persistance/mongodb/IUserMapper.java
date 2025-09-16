package kopo.poly.persistance.mongodb;


import kopo.poly.dto.UserDTO;

public interface IUserMapper {

    // 회원가입
    int CheckData(UserDTO pDTO, String colNm) throws Exception;

    int insertData(UserDTO pDTO, String colNm) throws Exception;

    // 로그인
    UserDTO GetLogin(UserDTO pDTO, String colNm) throws Exception;

    // 비밀번호 찾기
    int forgotPassword1(UserDTO pDTO, String colNm) throws Exception;

    int forgotPassword2(UserDTO pDTO, String colNm) throws Exception;

    // 회원탈퇴
    int deleteData(UserDTO pDTO, String colNm) throws Exception;

    // 기존 회원 비밀번호 변경 전 기본 비밀번호 일치 확인
    int checkCurrentPassword(UserDTO pDTO, String colNm) throws Exception;

    // [추가] userId로 사용자 정보를 DB에서 조회하는 메서드
    UserDTO getUserInfo(UserDTO pDTO, String colNm) throws Exception;

}

package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class UserDTO {


        @NotBlank(message = "아이디는 필수 입력 사항입니다.")
        @Size(max = 50, message = "아이디는 50자까지 입력 가능합니다.")
        private String userId;      // 회원아이디

        @NotBlank(message = "비밀번호는 필수 입력 사항입니다.")
        private String password;    // 비밀번호

        @NotBlank(message = "휴대폰번호는 필수 입력 사항입니다.")
        private String phoneNumber; // 전화번호

        private String regDt ;  // 회원가입일

        private String chgDt;   // 회원가입일

        private String adminYn; // 관리자여부    기본값 n, Y이면 관리자


}


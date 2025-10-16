package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class UserDTO implements Serializable {


        @NotBlank(message = "아이디는 필수 입력 사항입니다.")
        @Size(max = 50, message = "아이디는 50자까지 입력 가능합니다.")
        private String userId;      // 회원아이디

        private String password;    // 비밀번호

        private String phoneNumber; // 전화번호

        private String regDt ;  // 회원가입일

        private String chgDt;   // 회원 변경일

        private String adminYn; // 관리자여부    기본값 n, Y이면 관리자

        @Builder.Default
        private BigDecimal totalAmount = BigDecimal.ZERO; // 누적 결제 금액 필드 추가

        // --- 🚨 1. 여기에 provider 필드를 추가합니다. ---
        private String provider;


}


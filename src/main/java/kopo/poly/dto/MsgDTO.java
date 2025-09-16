package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record MsgDTO(

        int result, // 성공 : 1 / 실패 : 그 외

        String msg,  // 메시지

        Integer authNumber,  // 인증번호

        String userId,      // 세션 저장 아이디

        String regDt,       // 세션 가입일

        String adminYn      // 어드민 여부
) {
}

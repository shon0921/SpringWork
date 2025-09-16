package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.MsgDTO;
import kopo.poly.dto.UserDTO;
import kopo.poly.service.IUserService;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping(value = "/user")
@RequiredArgsConstructor
@RestController
public class UserController {

    private final IUserService userService;

    @PostMapping("/register1")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO pDTO, BindingResult bindingResult, HttpSession session) throws Exception {
        log.info("{}.register1 Start!", this.getClass().getName());

        if (bindingResult.hasErrors()) {
            return CommonResponse.getErrors(bindingResult);
        }

        log.info("Received MongoDTO : {}", pDTO);

        String originalPhoneNumber = pDTO.getPhoneNumber();

        UserDTO checkDTO = new UserDTO();
        checkDTO.setPhoneNumber(EncryptUtil.encAES128CBC(originalPhoneNumber));

        int res = userService.register(checkDTO);

        int authNumber = 0;
        String msg = "";

        if (res == 0) {
            UserDTO phoneDTO = new UserDTO();
            phoneDTO.setPhoneNumber(originalPhoneNumber);
            authNumber = userService.PhoneExists(phoneDTO);

            if (authNumber == -1) {
                msg = "메일 발송에 실패하였습니다\n휴대폰번호를 확인해주세요";
            } else {
                msg = "메일 발송에 성공하였습니다";

                String encPassword = EncryptUtil.encHashSHA256(pDTO.getPassword());
                String encPhone = EncryptUtil.encAES128CBC(originalPhoneNumber);
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                pDTO.setPassword(encPassword);
                pDTO.setPhoneNumber(encPhone);
                pDTO.setRegDt(now);
                pDTO.setChgDt(now);
                pDTO.setAdminYn("n");

                // DB 저장용 암호화된 pDTO 세션에 저장
                session.setAttribute("user", pDTO);

                // 원본 전화번호만 별도 세션에 저장 (SMS 발송, 인증에만 사용)
                session.setAttribute("userPhoneNumber", originalPhoneNumber);
            }
        } else if (res == 1) {
            msg = "아이디 중복";
        } else if (res == 2) {
            msg = "휴대폰 중복";
        }

        log.info("{}.register1 End!", this.getClass().getName());

        Map<String, Object> data = new HashMap<>();
        data.put("msg", msg);
        data.put("result", res);
        data.put("authNumber", res == 0 ? authNumber : 0);
        data.put("userId", pDTO.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resendAuthCode")
    public ResponseEntity<?> resendAuthCode(HttpSession session) throws Exception {
        log.info("{}.resendAuthCode Start!", this.getClass().getName());

        String phoneNumber = (String) session.getAttribute("userPhoneNumber");

        if (phoneNumber == null) {
            Map<String, Object> errorResp = new HashMap<>();
            errorResp.put("msg", "인증 정보가 존재하지 않습니다. 다시 인증을 진행해주세요.");
            errorResp.put("result", -1);
            return ResponseEntity.badRequest().body(errorResp);
        }

        UserDTO phoneDTO = new UserDTO();
        phoneDTO.setPhoneNumber(phoneNumber);
        int newAuthNumber = userService.PhoneExists(phoneDTO);

        String msg;
        if (newAuthNumber == -1) {
            msg = "인증번호 재발송에 실패했습니다. 휴대폰 번호를 확인해주세요.";
        } else {
            msg = "인증번호가 재발송되었습니다.";
        }

        Map<String, Object> data = new HashMap<>();
        data.put("msg", msg);
        data.put("result", newAuthNumber == -1 ? -1 : 0);
        data.put("newAuthNumber", newAuthNumber == -1 ? 0 : newAuthNumber);

        Map<String, Object> response = new HashMap<>();
        response.put("data", data);

        log.info("{}.resendAuthCode End!", this.getClass().getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register2")
    public ResponseEntity<?> register2(@RequestBody UserDTO pDTO, HttpSession session)
            throws Exception {

        log.info("{}.register2 Start!", this.getClass().getName());

        UserDTO updatedDTO = (UserDTO) session.getAttribute("user");
        // 로그 찍기
        log.info("Received UserDTO : {}", updatedDTO); // 입력 받은 값 확인하기

        session.setAttribute("user", updatedDTO);   //  세션에 저장

        // 3. MongoDB에 데이터 저장 시도
        int res = userService.register2(updatedDTO);

        // 4. 저장 결과 메시지 설정
        String msg = (res == 1) ? "회원가입에 성공하였습니다."  : "회원가입에 실패하였습니다";

        // 5. 결과 메시지를 DTO로 구성
        MsgDTO dto = MsgDTO.builder()
                .result(res)
                .msg(msg)
                .build();

        log.info("{}.register2 End!", this.getClass().getName());

        if (res == 1) {
            session.removeAttribute("user");        // 저장 성공 했으면 세션 제거
            session.removeAttribute("userPhoneNumber"); // 번호도 제거
        }


        // 6. 공통 응답 객체에 결과 담아 반환
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto));
    }

    // 로그인 처리
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO pDTO, HttpSession session) throws Exception {
        log.info("{}.login Start!", this.getClass().getName());

        // 비밀번호 SHA256 암호화
        pDTO.setPassword(EncryptUtil.encHashSHA256(pDTO.getPassword()));

        UserDTO loginUser = userService.login(pDTO);

        if (loginUser != null) {
            // 로그인 성공, 세션에 유저 정보 저장
            session.setAttribute("userId", loginUser.getUserId());
            session.setAttribute("regDt", loginUser.getRegDt());
            session.setAttribute("adminYn", loginUser.getAdminYn());

            log.info("{}.login Success: session set userId={}, regDt={}, adminYn={}",
                    this.getClass().getName(),
                    loginUser.getUserId(),
                    loginUser.getRegDt(),
                    loginUser.getAdminYn());

            MsgDTO dto = MsgDTO.builder()
                    .result(1)
                    .msg("로그인 성공")
                    .userId(loginUser.getUserId())
                    .regDt(loginUser.getRegDt())
                    .adminYn(loginUser.getAdminYn())
                    .build();

            log.info("{}.login End! Returning success response", this.getClass().getName());
            return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", dto));
        } else {
            // 로그인 실패
            log.warn("{}.login Failed for userId: {}", this.getClass().getName(), pDTO.getUserId());

            MsgDTO dto = MsgDTO.builder()
                    .result(0)
                    .msg("로그인 실패")
                    .userId(pDTO.getUserId())
                    .build();

            log.info("{}.login End! Returning failure response", this.getClass().getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.of(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", dto));
        }
    }

    // 비밀번호 찾기1
    @PostMapping("/forgotpassword1")
    public ResponseEntity<?> forgotpassword1(@RequestBody UserDTO pDTO, HttpSession session) throws Exception {

        log.info("{}.forgotpassword1 Start!", this.getClass().getName());

        UserDTO rDTO = new UserDTO();

        rDTO.setUserId(pDTO.getUserId());
        rDTO.setPhoneNumber(EncryptUtil.encAES128CBC(pDTO.getPhoneNumber()));

        log.info("{}. {}", rDTO.getUserId(), rDTO.getPhoneNumber());

        int res = userService.forgotNumber1(rDTO); // 사용자 정보 조회

        log.info("{} res 값",res);

        String msg = (res == 1) ? "비밀번호 찾기 성공" : "비밀번호 찾기 실패";

        int authNumber = 0;

        if (res == 1) {
            authNumber = userService.forgotNumber2(rDTO);

            if(authNumber == -1) {
                msg = "메일 발송에 실패하였습니다\n휴대폰번호를 확인해주세요";
            } else {
                msg = "메일 발송에 성공하였습니다";

                pDTO.setUserId(pDTO.getUserId());
                session.setAttribute("userId", rDTO.getUserId());

                UserDTO dDTO = new UserDTO();
                dDTO.setPhoneNumber(pDTO.getPhoneNumber());       // 휴대폰 번호용 DTO 만들기

                session.setAttribute("userPhoneNumber", dDTO.getPhoneNumber()); // 세션에 저장

            }

        }

        Map<String, Object> data = new HashMap<>();
        data.put("msg", msg);
        data.put("result", res);
        data.put("authNumber", res == 1 ? authNumber : -1);
        data.put("userId", pDTO.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("data", data);

        log.info("{}.forgotpassword1 End!", this.getClass().getName());

        return ResponseEntity.ok(response);
    }

    // 비밀번호 찾기2
    @PostMapping("/forgotpassword2")
    public ResponseEntity<?> forgotpassword2(@RequestBody UserDTO pDTO, HttpSession session) throws Exception {

        log.info("{}.forgotpassword2 Start!", this.getClass().getName());

        log.info("입력한 비밀번호 : {}", pDTO.getPassword());

        UserDTO rDTO = new UserDTO();

        rDTO.setUserId(session.getAttribute("userId").toString());
        rDTO.setPassword(EncryptUtil.encHashSHA256(pDTO.getPassword()));
        rDTO.setChgDt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        log.info("{} {}", rDTO.getUserId(), rDTO.getPassword());

        int res = userService.forgotNumber3(rDTO); // 비밀번호 교체를 위한 조회

        String msg = "";

        if (res == 1) {
            msg ="비밀번호 변경 성공";

            session.removeAttribute("user");
            session.removeAttribute("userId");
            session.removeAttribute("userPhoneNumber");
        } else {
            msg ="비밀번호 변경 실패";
        }

        MsgDTO dto = MsgDTO.builder()
                .result(res)
                .msg(msg)
                .build();

        log.info("{}.forgotpassword3 End!", this.getClass().getName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(),dto));
    }

    // 로그아웃
    @PostMapping(value = "logout")
    public ResponseEntity<?> logout(HttpSession session) throws Exception {

        log.info("{}.logout Start!", this.getClass().getName());

        // 세션 무효화
        session.invalidate();

        // 로그아웃 성공 메시지
        String msg = "로그아웃에 성공했습니다.";

        log.info("{}.logout End!", this.getClass().getName());

        // 결과 객체 생성
        Map<String, Object> response = new HashMap<>();
        response.put("msg", msg); // 메시지 추가
        response.put("result", 1); // 로그아웃 성공 시 1로 설정

        return ResponseEntity.ok().body(response);
    }

    // 회원탈퇴
    @PostMapping(value = "delete")
    public ResponseEntity<?> delete( @RequestBody UserDTO pDTO, HttpSession session) throws Exception {

        log.info("{}.delete Start!", this.getClass().getName());

        UserDTO rDTO = new UserDTO();
        rDTO.setUserId(session.getAttribute("userId").toString());
        rDTO.setPassword(EncryptUtil.encHashSHA256(pDTO.getPassword()));

        log.info("{}.{}", rDTO.getUserId(), rDTO.getPassword());

        int res = userService.accountDelete(rDTO);
        String msg;

        if (res == 1) {
            msg = "회원탈퇴 성공";

            // 세션 무효화
            session.invalidate();
        } else {
            msg ="회원탈퇴 실패";
        }

        log.info("{}.delete End!", this.getClass().getName());
        // 결과 객체 생성
        Map<String, Object> response = new HashMap<>();
        response.put("msg", msg); // 메시지 추가
        response.put("result", res); // 로그아웃 성공 시 1로 설정

        return ResponseEntity.ok().body(response);
    }

    // 기존 비밀번호 변경
    @PostMapping("/changepassword")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, Object> pMap, HttpSession session) throws Exception {
        log.info("changePassword Start!");

        String userId = (String) session.getAttribute("userId");

        // 암호화된 기존 비밀번호와 새로운 비밀번호 추출
        String oldPassword = EncryptUtil.encHashSHA256((String) pMap.get("apassword"));
        String newPassword = EncryptUtil.encHashSHA256((String) pMap.get("password"));

        UserDTO pDTO = new UserDTO();
        pDTO.setUserId(userId);
        pDTO.setPassword(oldPassword); // 기존 비밀번호 확인용

        int isMatch = userService.checkCurrentPassword(pDTO);   // 기본 비밀번호 확인

        if (isMatch != 1) {
            MsgDTO dto = MsgDTO.builder()
                    .result(0)
                    .msg("기존 비밀번호가 일치하지 않습니다.")
                    .build();
            return ResponseEntity.ok(CommonResponse.of(HttpStatus.BAD_REQUEST, "FAILURE", dto));
        }

        // 새 비밀번호로 변경
        UserDTO rDTO = new UserDTO();
        rDTO.setUserId(userId);
        rDTO.setPassword(newPassword);
        rDTO.setChgDt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        int res = userService.forgotNumber3(rDTO);

        String msg = (res == 1) ? "비밀번호 변경 성공" : "비밀번호 변경 실패";

        MsgDTO dto = MsgDTO.builder()
                .result(res)
                .msg(msg)
                .build();

        log.info("changePassword End!");
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "OK", dto));
    }
}

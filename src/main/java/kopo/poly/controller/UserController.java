package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.MsgDTO;
import kopo.poly.dto.UserDTO;
import kopo.poly.service.IUserService;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO pDTO, HttpSession session) throws Exception {
        log.info("{}.login Start!", this.getClass().getName());

        // 비밀번호 SHA256 해시
        pDTO.setPassword(EncryptUtil.encHashSHA256(pDTO.getPassword()));

        UserDTO loginUser = userService.login(pDTO);

        if (loginUser != null) {
            // 세션 저장
            session.setAttribute("userId", loginUser.getUserId());
            session.setAttribute("regDt", loginUser.getRegDt());
            session.setAttribute("adminYn", loginUser.getAdminYn());

            // ✅ Spring Security에도 로그인 상태 저장
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    loginUser.getUserId(), // principal
                    null, // credentials (비밀번호는 저장 안 함)
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")) // 권한
            );

            // 1. SecurityContext 가져오기
            SecurityContext sc = SecurityContextHolder.getContext();
            // 2. Authentication 객체 저장
            sc.setAuthentication(authentication);
            // 3. HttpSession에 SecurityContext 직접 저장 ⭐ (핵심 수정)
            session.setAttribute("SPRING_SECURITY_CONTEXT", sc);

            Map<String, Object> data = new HashMap<>();
            data.put("result", 1);
            data.put("msg", "로그인 성공");
            data.put("userId", loginUser.getUserId());
            data.put("regDt", loginUser.getRegDt());
            data.put("adminYn", loginUser.getAdminYn());
            data.put("redirectUrl", "/main/title");

            log.info("{}" ,data);


            return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", data));
        } else {
            MsgDTO dto = MsgDTO.builder()
                    .result(0)
                    .msg("아이디 또는 비밀번호가 일치하지 않습니다.")
                    .userId(pDTO.getUserId())
                    .build();

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

//    // 로그아웃
//    @PostMapping(value = "logout")
//    public ResponseEntity<?> logout(HttpSession session) throws Exception {
//
//        log.info("{}.logout Start!", this.getClass().getName());
//
//        // 세션 무효화
//        session.invalidate();
//
//        // 로그아웃 성공 메시지
//        String msg = "로그아웃에 성공했습니다.";
//
//        log.info("{}.logout End!", this.getClass().getName());
//
//        // 결과 객체 생성
//        Map<String, Object> response = new HashMap<>();
//        response.put("msg", msg); // 메시지 추가
//        response.put("result", 1); // 로그아웃 성공 시 1로 설정
//
//        return ResponseEntity.ok().body(response);
//    }

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

    /**
     * 내 정보 보기 API
     */
    @GetMapping("/info")
    public ResponseEntity<CommonResponse> getUserInfo(HttpSession session) throws Exception {

        log.info("{}.getUserInfo Start!", this.getClass().getName());

        String userId = (String) session.getAttribute("userId");

        if (userId == null) {
            // 세션에 userId가 없으면 로그인되지 않은 상태로 간주
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.of(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "로그인이 필요합니다."));
        }

        // 사용자 정보 조회
        UserDTO pDTO = userService.getUserInfo(userId);

        if (pDTO != null) {
            // DB에 암호화되어 저장된 전화번호를 복호화
            pDTO.setPhoneNumber(EncryptUtil.decAES128CBC(pDTO.getPhoneNumber()));
        } else {
            // 사용자 정보가 없는 경우 (오류 상황)
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.of(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다."));
        }

        log.info("{}.getUserInfo End!", this.getClass().getName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), pDTO));
    }

    /**
     * 소셜 로그인 회원 탈퇴 (비밀번호 확인 없음)
     */
    @PostMapping(value = "delete-social") // '/user' 접두사는 클래스 레벨에 @RequestMapping("/user")로 설정되어 있어야 합니다.
    @ResponseBody
    public MsgDTO deleteSocialUser(HttpSession session) throws Exception {

        log.info(this.getClass().getName() + ".deleteSocialUser Start!");

        // 1. 세션에서 사용자 ID 가져오기
        String userId = CmmUtil.nvl((String) session.getAttribute("userId"));
        log.info("userId : " + userId);

        // 2. 로그인 상태가 아니면, 에러 메시지를 Builder로 생성하여 바로 반환
        if (userId.isEmpty()) {
            return MsgDTO.builder().msg("로그인 정보가 없습니다.").result(0).build();
        }

        // 3. pDTO 생성 및 서비스 호출
        UserDTO pDTO = UserDTO.builder().userId(userId).build();

        // 🚨 서비스 메서드 이름을 'deleteUser'로 수정
        int result = userService.accountDelete(pDTO);

        // 4. 결과에 따라 Builder로 MsgDTO 생성 및 반환
        if (result == 1) {
            session.invalidate(); // 세션 초기화
            log.info("Social user '{}' deleted successfully.", userId);
            return MsgDTO.builder().msg("회원탈퇴가 완료되었습니다.").result(1).build();
        } else {
            log.warn("Failed to delete social user '{}'.", userId);
            // 비밀번호 불일치 메시지 대신 일반적인 실패 메시지로 수정
            return MsgDTO.builder().msg("회원탈퇴에 실패했습니다.").result(0).build();
        }
    }
}
package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.PostDTO;
import kopo.poly.service.impl.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {

    private final PostService postService;

    /**
     * 사용자가 조회한 배송 결과를 DB에 저장합니다.
     *
     * @param dto     프론트엔드에서 전달된 배송 정보 DTO
     * @param session 현재 세션
     * @return 처리 결과를 담은 Map 객체 (SUCCESS, LOGIN_REQUIRED, ALREADY_SAVED)
     */
    @PostMapping("/saveLog")
    @ResponseBody
    public Map<String, Object> saveDeliveryLog(@RequestBody PostDTO dto, HttpSession session) throws Exception {
        log.info("{}.saveDeliveryLog Start!", this.getClass().getName());

        Map<String, Object> result = new HashMap<>();
        String userId = (String) session.getAttribute("userId");

        // 1. 로그인 여부 확인
        if (userId == null) {
            log.warn("로그인되지 않은 사용자의 저장 시도입니다.");
            result.put("message", "LOGIN_REQUIRED");
            return result;
        }

        // 2. DTO에 사용자 ID와 등록일시 설정
        dto.setUserId(userId);
        dto.setRegDt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("사용자 '{}'의 배송 정보 저장을 시작합니다. (운송장번호: {})", userId, dto.getTrackingNumber());

        // 3. API에서 받은 ISO 8601 형식의 날짜를 DB 저장 형식(yyyy-MM-dd HH:mm)으로 변환
        if (dto.getLastDeliveryTime() != null && !dto.getLastDeliveryTime().isEmpty()) {
            try {
                // ISO 형식의 문자열을 ZonedDateTime으로 파싱
                ZonedDateTime zdt = ZonedDateTime.parse(dto.getLastDeliveryTime());
                // 원하는 형식으로 포맷팅
                String formatted = zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                dto.setLastDeliveryTime(formatted);
            } catch (DateTimeParseException e) {
                // 혹시 모를 잘못된 형식의 날짜 문자열에 대비한 예외 처리
                log.warn("lastDeliveryTime 파싱 실패. 원본 값: {}", dto.getLastDeliveryTime());
                dto.setLastDeliveryTime(null); // 파싱 실패 시 null로 설정하여 저장
            }
        } else {
            // lastDeliveryTime이 null이거나 빈 문자열이면, DB에도 null로 저장되도록 명시적으로 설정
            dto.setLastDeliveryTime(null);
        }

        // 4. 이미 저장된 기록인지 확인
        log.info("기존 저장 내역 확인 중... (사용자: {}, 주소: {}, 운송장: {})", userId, dto.getToAddress(), dto.getTrackingNumber());
        boolean exists = postService.existsByUserId(userId, dto.getToAddress(), dto.getTrackingNumber());

        if (exists) {
            log.info("이미 저장된 배송 정보입니다. 저장을 중단합니다.");
            result.put("message", "ALREADY_SAVED");
            return result;
        }

        // 5. 서비스 호출하여 DB에 저장
        postService.saveLog(dto);
        log.info("배송 정보 저장을 완료했습니다.");
        result.put("message", "SUCCESS");

        log.info("{}.saveDeliveryLog End!", this.getClass().getName());
        return result;
    }

    /**
     * 현재 로그인한 사용자가 특정 배송 기록을 이미 저장했는지 확인합니다.
     *
     * @param toAddress      받는 사람 주소
     * @param trackingNumber 운송장 번호
     * @param session        현재 세션
     * @return 저장되어 있으면 true, 아니면 false
     */
    @GetMapping("/checkLogExist")
    @ResponseBody
    public ResponseEntity<Boolean> checkLogExist(
            @RequestParam String toAddress,
            @RequestParam String trackingNumber,
            HttpSession session) throws Exception {
        log.info("{}.checkLogExist Start!", this.getClass().getName());

        String userId = (String) session.getAttribute("userId");
        // 로그인하지 않은 경우, 저장된 기록이 있을 수 없으므로 false 반환
        if (userId == null) {
            log.info("비로그인 사용자, 저장 여부 확인 결과 false 반환");
            return ResponseEntity.ok(false);
        }

        boolean exists = postService.existsByUserId(userId, toAddress, trackingNumber);
        log.info("저장 여부 확인 결과: {} (사용자: {}, 운송장: {})", exists, userId, trackingNumber);

        log.info("{}.checkLogExist End!", this.getClass().getName());
        return ResponseEntity.ok(exists);
    }

    /**
     * 저장된 배송 기록을 삭제합니다.
     *
     * @param dto     삭제할 배송 정보 (toAddress, trackingNumber 필요)
     * @param session 현재 세션
     * @return 처리 결과를 담은 Map 객체 (SUCCESS, LOGIN_REQUIRED)
     */
    @DeleteMapping("/deleteLog")
    @ResponseBody
    public Map<String, Object> deleteDeliveryLog(@RequestBody PostDTO dto, HttpSession session) {
        log.info("{}.deleteDeliveryLog Start!", this.getClass().getName());

        Map<String, Object> result = new HashMap<>();
        String userId = (String) session.getAttribute("userId");

        // 1. 로그인 여부 확인
        if (userId == null) {
            log.warn("로그인되지 않은 사용자의 삭제 시도입니다.");
            result.put("message", "LOGIN_REQUIRED");
            return result;
        }

        log.info("사용자 '{}'의 배송 정보 삭제를 시작합니다. (운송장번호: {})", userId, dto.getTrackingNumber());

        // 2. 서비스 호출하여 DB에서 삭제
        postService.deleteByUserId(userId, dto.getToAddress(), dto.getTrackingNumber());

        log.info("배송 정보 삭제를 완료했습니다.");
        result.put("message", "SUCCESS");

        log.info("{}.deleteDeliveryLog End!", this.getClass().getName());
        return result;
    }

    /**
     * 현재 로그인한 사용자의 저장된 모든 배송 목록을 조회합니다.
     *
     * @param session 현재 HttpSession
     * @return 성공 시 배송 목록 데이터, 실패 시 오류 메시지를 포함한 ResponseEntity 객체
     */
    @GetMapping("/getLogList")
    @ResponseBody
    public ResponseEntity<?> getLogList(HttpSession session) {
        log.info("{}.getLogList Start!", this.getClass().getName());

        // 1. 세션에서 현재 로그인한 사용자 ID 가져오기
        String userId = (String) session.getAttribute("userId");

        // 2. 비로그인 사용자인 경우, 401 Unauthorized 응답 반환
        if (userId == null) {
            log.warn("비로그인 사용자가 저장된 배송 목록 조회를 시도했습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.of(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다.", null));
        }

        try {
            // 3. 서비스 계층을 호출하여 해당 사용자의 저장된 목록 가져오기
            log.info("사용자 '{}'의 저장된 배송 목록 조회를 시작합니다.", userId);
            List<PostDTO> logList = postService.getLogListByUserId(userId);

            log.info("사용자 '{}'의 저장된 배송 목록 {}건을 성공적으로 조회했습니다.", userId, logList.size());

            // 4. 성공 시, 200 OK 상태와 함께 조회된 데이터를 응답
            return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", logList));

        } catch (Exception e) {
            log.error("ID '{}'의 저장된 배송 목록 조회 중 서버 오류 발생", userId, e);

            // 5. 처리 중 예외 발생 시, 500 Internal Server Error 응답 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", null));
        } finally {
            log.info("{}.getLogList End!", this.getClass().getName());
        }
    }

    /**
     * 특정 배송 기록 하나를 상세 조회합니다. (저장된 목록에서 항목 클릭 시 사용)
     *
     * @param toAddress      받는 사람 주소
     * @param trackingNumber 운송장 번호
     * @param session        현재 세션
     * @return 성공 시 단일 배송 정보, 실패 시 오류 상태 코드를 포함한 ResponseEntity 객체
     */
    @GetMapping("/getLogByTrackingInfo")
    @ResponseBody
    public ResponseEntity<?> getLogByTrackingInfo(
            @RequestParam String toAddress,
            @RequestParam String trackingNumber,
            HttpSession session) {
        log.info("{}.getLogByTrackingInfo Start!", this.getClass().getName());

        String userId = (String) session.getAttribute("userId");

        // 1. 로그인 여부 확인
        if (userId == null) {
            log.warn("비로그인 사용자가 단일 배송 정보 조회를 시도했습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        try {
            log.info("단일 배송 정보 조회 시작 (사용자: {}, 운송장: {})", userId, trackingNumber);

            // 2. 서비스 호출하여 단일 배송 기록 조회
            PostDTO postDto = postService.getLogByTrackingInfo(userId, toAddress, trackingNumber);

            if (postDto != null) {
                log.info("조회 성공. 운송장 번호: {}", postDto.getTrackingNumber());
                return ResponseEntity.ok(postDto);
            } else {
                // 해당 조건으로 저장된 기록이 없는 경우
                log.warn("조회 실패. 해당 조건의 저장된 배송 정보가 없습니다. (사용자: {}, 운송장: {})", userId, trackingNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            log.error("단일 로그 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } finally {
            log.info("{}.getLogByTrackingInfo End!", this.getClass().getName());
        }
    }

    /**
     * 특정 배송 기록의 즐겨찾기 상태를 업데이트합니다.
     *
     * @param dto     toAddress, trackingNumber, Favorite를 포함한 DTO
     * @param session 현재 세션
     * @return 처리 결과 메시지
     */
    @PostMapping("/updateFavorite")
    @ResponseBody
    public Map<String, Object> updateFavorite(@RequestBody PostDTO dto, HttpSession session) {
        log.info("{}.updateFavorite Start!", this.getClass().getName());

        Map<String, Object> result = new HashMap<>();
        String userId = (String) session.getAttribute("userId");

        if (userId == null) {
            log.warn("로그인되지 않은 사용자의 즐겨찾기 변경 시도");
            result.put("message", "LOGIN_REQUIRED");
            return result;
        }

        try {
            dto.setUserId(userId);
            // PostService에 즐겨찾기 업데이트 메소드 추가 필요
            postService.updateFavorite(dto);
            log.info("즐겨찾기 상태 업데이트 완료 (운송장: {}, 상태: {})", dto.getTrackingNumber(), dto.getFavorites());
            result.put("message", "SUCCESS");
        } catch (Exception e) {
            log.error("즐겨찾기 상태 업데이트 중 오류 발생", e);
            result.put("message", "ERROR");
        }

        log.info("{}.updateFavorite End!", this.getClass().getName());
        return result;
    }
}
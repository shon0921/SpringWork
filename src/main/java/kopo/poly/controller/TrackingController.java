package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.GeminiRequest;
import kopo.poly.dto.GeminiResponse;
import kopo.poly.dto.TrackingDTO;
import kopo.poly.dto.TrackingRequestDTO;
import kopo.poly.service.ITrackingService;
import kopo.poly.service.feign.GeminiAPIService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 배송 조회 관련 요청을 처리하는 컨트롤러입니다.
 * 외부 배송 조회 API를 호출하고, 그 결과를 세션에 저장하여 프론트엔드에 제공하는 역할을 합니다.
 */
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/tracking")
@RestController
public class TrackingController {

    private final ITrackingService trackingService;
    private final GeminiAPIService geminiAPIService; // Feign Client 주입

    @Value("${gemini.api.key}") // application.properties에서 API 키를 가져옵니다.
    private String geminiApiKey;

    // 외부 배송 조회 API의 기본 URL
    private static final String API_BASE_URL = "https://apis.tracker.delivery/carriers/";

    @PostMapping("/get-refined-address")
    public ResponseEntity<Map<String, String>> getRefinedAddress(@RequestBody Map<String, String> request) {
        log.info(this.getClass().getName() + ".getRefinedAddress Start!");

        String ambiguousTerm = request.get("ambiguousTerm");
        String prompt = String.format(
                "당신은 한국의 주요 지도 서비스(네이버 지도, 카카오맵)에서 장소를 검색하는 데 최적화된 주소 변환 전문가입니다. " +
                        "사용자가 입력한 모호하거나 약칭으로 된 택배 터미널, 지점, 우체국 등의 위치 키워드를 지도에서 가장 잘 검색되는 공식 명칭이나 가장 일반적으로 통용되는 명칭으로 변환하는 임무를 받았습니다.\n\n" +
                        "## 규칙:\n" +
                        "1. 결과는 반드시 하나의 장소 이름이어야 합니다.\n" +
                        "2. 부가적인 설명, 인사, 줄바꿈, 따옴표 없이 변환된 명칭만 정확히 출력해야 합니다.\n" +
                        "3. 변환이 불가능하거나 키워드가 너무 불분명하여 추측하기 어려우면, 원래의 입력값을 그대로 출력해주세요.\n\n" +
                        "## 예시:\n" +
                        "- 입력: 곤지암hub, 출력: CJ대한통운 곤지암HUB\n" +
                        "- 입력: 대전HUB, 출력: CJ대한통운 대전HUB\n" +
                        "- 입력: 부산동래 롯데, 출력: 롯데택배 동래지점\n" +
                        "- 입력: 서울광진 cj, 출력: CJ대한통운 광진지점\n" +
                        "- 입력: 평택우체국, 출력: 평택우체국\n\n" +
                        "## 변환 시작:\n" +
                        "입력: %s, 출력:",
                ambiguousTerm
        );

        try {
            // 1️⃣ GeminiRequest 구성 (2.0-flash 형식)
            GeminiRequest geminiRequest = new GeminiRequest(
                    List.of(new GeminiRequest.Content(
                            List.of(new GeminiRequest.Part(prompt))
                    ))
            );

            // 2️⃣ 2.0 모델명 사용
            String modelName = "gemini-2.0-flash";

            // 3️⃣ Feign 호출
            GeminiResponse geminiResponse = geminiAPIService.generateContent(modelName, geminiApiKey, geminiRequest);

            // 4️⃣ 응답 텍스트 추출 (candidates → content → parts → text)
            String refinedAddress = geminiResponse.getCandidates()
                    .get(0)
                    .getContent()      // Content 객체
                    .getParts()        // List<Part>
                    .get(0)
                    .getText()
                    .trim();

            log.info("Refined Address: {}", refinedAddress);
            log.info(this.getClass().getName() + ".getRefinedAddress End!");
            return ResponseEntity.ok(Map.of("refinedAddress", refinedAddress));

        } catch (Exception e) {
            log.error("Gemini 2.0 API 호출 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 기본 배송 조회를 수행하고, 결과를 세션에 저장합니다.
     * (주로 비회원 또는 초기 조회 시 사용)
     *
     * @param request carrierId, trackingNumber를 포함하는 요청 본문
     * @param session 현재 HttpSession
     * @return 처리 결과를 담은 Map 객체 (항상 {"result": "OK"} 반환)
     */
    @PostMapping("/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchTracking(@RequestBody Map<String, String> request, HttpSession session) {
        log.info("{}.searchTracking Start!", this.getClass().getName());

        try {
            String carrierId = request.get("carrierId");
            String trackingNumber = request.get("trackingNumber");

            log.info("배송 조회 요청 수신 (기본) -> 택배사 ID: {}, 송장번호: {}", carrierId, trackingNumber);

            // 1. 외부 API를 호출하여 배송 정보 조회
            log.info("TrackingService 호출하여 외부 API 배송 정보 조회를 시작합니다.");
            Map<String, Object> trackingInfo = trackingService.fetchTrackingInfo(carrierId, trackingNumber);

            // 2. 응답 데이터에 송장번호 추가 (세션에 함께 저장하기 위함)
            trackingInfo.put("trackingNumber", trackingNumber);
            log.info("외부 API 응답 데이터를 가공했습니다. (송장번호 추가)");

            // 3. 조회된 전체 정보를 세션에 'trackingInfo'라는 키로 저장
            session.setAttribute("trackingInfo", trackingInfo);
            log.info("가공된 배송 정보를 세션에 저장했습니다.");

            log.info("{}.searchTracking End! - 성공", this.getClass().getName());

            // 4. 성공 시: 프론트엔드에는 성공 신호와 200 OK 상태 반환
            return ResponseEntity.ok(Map.of("result", "OK"));

        } catch (Exception e) {
            // 5. 실패 시: 서비스에서 발생한 예외를 잡아서 실패 신호와 오류 메시지, 400 Bad Request 상태 반환
            log.error("배송 조회 처리 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("result", "FAIL");
            errorResponse.put("message", e.getMessage()); // 서비스에서 던진 예외 메시지를 그대로 전달

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * [수정] 서비스로부터 TrackingDTO를 받아 클라이언트에게 직접 반환합니다.
     *       ResponseEntity의 제네릭 타입을 TrackingDTO로 명시합니다.
     */
    @PostMapping("/search2")
    public ResponseEntity<CommonResponse<TrackingDTO>> searchTracking2(@RequestBody @Valid TrackingRequestDTO pDTO) {

        log.info("------------------------------------------------------------");
        log.info("{}.searchTracking2 Start!", this.getClass().getName());

        log.info("배송 조회 요청 수신 -> 택배사 ID: {}, 송장번호: {}, 주소: {}",
                pDTO.getCarrierId(), pDTO.getTrackingNumber(), pDTO.getAddr1());

        try {
            // 1. 서비스 계층을 호출하여 외부 API로부터 배송 정보를 TrackingDTO 객체로 가져옵니다.
            log.info("TrackingService를 호출하여 외부 API 조회를 시작합니다.");
            TrackingDTO trackingDTO = trackingService.fetchTrackingInfo2(pDTO.getCarrierId(), pDTO.getTrackingNumber());

            // DTO 자체에는 toAddress 필드가 없으므로, 이 정보는 프론트엔드에서 이미 알고 있습니다.
            // 따라서 별도로 추가할 필요가 없습니다. 클라이언트는 요청 시 보냈던 주소를 그대로 사용하면 됩니다.

            log.info("클라이언트에게 성공(200 OK) 응답과 함께 조회된 TrackingDTO를 반환합니다.");
            log.info("{}.searchTracking2 End!", this.getClass().getName());
            log.info("------------------------------------------------------------");

            // 2. 성공 응답(200 OK)과 함께 조회된 DTO를 CommonResponse로 감싸서 반환합니다.
            return ResponseEntity.ok(
                    CommonResponse.of(HttpStatus.OK, "SUCCESS", trackingDTO)
            );

        } catch (HttpClientErrorException e) {
            log.warn("외부 API 호출 중 클라이언트 오류(4xx) 발생. Status: {}, Message: {}", e.getStatusCode(), e.getMessage());
            log.info("{}.searchTracking2 End! (API Client Error)", this.getClass().getName());
            log.info("------------------------------------------------------------");

            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(CommonResponse.of(HttpStatus.valueOf(e.getStatusCode().value()), "조회된 배송 정보가 없습니다.", null));

        } catch (Exception e) {
            log.error("배송 정보 조회 중 예측하지 못한 서버 오류가 발생했습니다.", e);
            log.info("{}.searchTracking2 End! (Internal Server Error)", this.getClass().getName());
            log.info("------------------------------------------------------------");

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", null));
        }
    }

    /**
     * 배송 조회 결과 페이지로 이동시키는 단순 페이지 매핑 메서드입니다.
     * (서버 사이드 렌더링 환경에서 사용)
     *
     * @return 렌더링할 뷰의 이름 ("trackingResult")
     */
    @GetMapping("/result")
    public String showTrackingResult() {
        log.info("{}.showTrackingResult Start!", this.getClass().getName());
        log.info("배송 조회 결과 페이지(trackingResult.html)로 이동합니다.");
        log.info("{}.showTrackingResult End!", this.getClass().getName());
        return "trackingResult";
    }

    /**
     * 세션에 저장된 배송 정보(trackingInfo)를 그대로 반환합니다.
     *
     * @param session 현재 HttpSession
     * @return 세션에 저장된 배송 정보 객체
     */
    @GetMapping("/sessionData")
    @ResponseBody
    public Object getSessionData(HttpSession session) {
        log.info("{}.getSessionData Start!", this.getClass().getName());
        Object trackingInfo = session.getAttribute("trackingInfo");
        if (trackingInfo == null) {
            log.warn("세션에서 'trackingInfo'를 찾을 수 없습니다.");
        } else {
            log.info("세션에서 'trackingInfo' 데이터를 성공적으로 가져왔습니다.");
        }
        log.info("{}.getSessionData End!", this.getClass().getName());
        return trackingInfo;
    }

    /**
     * 세션에 저장된 배송 정보 중 프론트엔드에 필요한 데이터만 선별하여 반환합니다.
     *
     * @param session 현재 HttpSession
     * @return 선별된 배송 정보 데이터를 담은 Map 객체
     */
    @GetMapping("/sessionDataWithAddress")
    @ResponseBody
    public Map<String, Object> getSessionDataWithAddress(HttpSession session) {
        log.info("{}.getSessionDataWithAddress Start!", this.getClass().getName());

        Object trackingInfoObj = session.getAttribute("trackingInfo");

        // 1. 세션 데이터 유효성 검사
        if (trackingInfoObj == null || !(trackingInfoObj instanceof Map)) {
            log.warn("세션에 'trackingInfo' 데이터가 없거나 형식이 올바르지 않습니다.");
            return Map.of("error", "No tracking info available");
        }

        @SuppressWarnings("unchecked") // 타입 변환 안전성 경고 무시
        Map<String, Object> trackingInfo = (Map<String, Object>) trackingInfoObj;
        log.info("세션 데이터를 Map 형태로 변환했습니다.");

        // 2. 프론트엔드에 필요한 데이터만 담을 새로운 Map 생성
        Map<String, Object> result = new HashMap<>();
        result.put("carrier", trackingInfo.get("carrier"));
        result.put("from", trackingInfo.get("from"));
        result.put("to", trackingInfo.get("to"));
        result.put("state", trackingInfo.get("state"));
        result.put("progresses", trackingInfo.get("progresses"));

        // 3. 'toAddress'와 'trackingNumber'는 세션에 별도로 추가되었으므로, 명시적으로 꺼내서 담아줌
        if (trackingInfo.get("toAddress") != null) {
            result.put("toAddress", trackingInfo.get("toAddress"));
        } else {
            result.put("toAddress", ""); // 값이 없을 경우를 대비한 기본값
        }

        if (trackingInfo.get("trackingNumber") != null) {
            result.put("trackingNumber", trackingInfo.get("trackingNumber"));
        } else {
            result.put("trackingNumber", ""); // 값이 없을 경우를 대비한 기본값
        }

        log.info("프론트엔드로 보낼 응답 데이터 재구성을 완료했습니다. (포함된 키: {})", result.keySet());
        log.info("{}.getSessionDataWithAddress End!", this.getClass().getName());

        return result;
    }
}

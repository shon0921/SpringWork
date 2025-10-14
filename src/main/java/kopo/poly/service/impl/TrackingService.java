package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.TrackingDTO;
import kopo.poly.service.ITrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;



/**
 * 외부 배송 조회 API와의 통신을 담당하는 서비스 클래스입니다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TrackingService implements ITrackingService {

    private static final String API_BASE_URL = "https://apis.tracker.delivery/carriers/";

    // RestTemplate과 ObjectMapper는 애플리케이션에서 Bean으로 등록하여 주입받는 것이 권장됩니다.
    // 여기서는 기존 로직 유지를 위해 그대로 둡니다.
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 외부 배송 조회 API를 호출하여 결과를 타입 변환 없이 Map 형태로 반환합니다.
     *
     * @param carrierId      택배사 ID
     * @param trackingNumber 운송장 번호
     * @return API 응답을 변환한 Map 객체
     * @throws RuntimeException JSON 파싱 실패 시
     */
    @Override
    public Map<String, Object> fetchTrackingInfo(String carrierId, String trackingNumber) {
        log.info("{}.fetchTrackingInfo Start!", this.getClass().getName());
        log.info("외부 API 조회 요청 (Raw Map) -> 택배사 ID: {}, 송장번호: {}", carrierId, trackingNumber);

        String url = String.format("https://apis.tracker.delivery/carriers/%s/tracks/%s", carrierId, trackingNumber);
        log.info("외부 배송 조회 API 호출 URL: {}", url);

        try {
            // 1. RestTemplate을 사용하여 GET 요청 및 응답(JSON 문자열) 수신
            String response = restTemplate.getForObject(url, String.class);
            log.info("외부 API로부터 응답을 수신했습니다.");

            // 2. 수신된 JSON 문자열을 Map<String, Object> 형태로 변환
            Map<String, Object> resultMap = objectMapper.readValue(response, Map.class);
            log.info("JSON 응답을 Map<String, Object>으로 변환 성공.");

            log.info("{}.fetchTrackingInfo End!", this.getClass().getName());
            return resultMap;

        } catch (HttpClientErrorException e) {
            // 4xx 오류 (예: 404 Not Found) 발생 시
            log.error("외부 API 호출 중 클라이언트 오류 발생. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            // 사용자에게 보여줄 명확한 메시지를 담아 예외를 던짐
            throw new RuntimeException("배송 정보를 찾을 수 없습니다. 택배사 또는 송장번호를 다시 확인해주세요.");
        } catch (Exception e) {
            // 그 외 모든 예외 (JSON 파싱 오류, 서버 오류 등)
            log.error("배송 정보 조회 중 예측하지 못한 오류 발생", e);
            throw new RuntimeException("배송 정보 조회에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    @Override
    public TrackingDTO fetchTrackingInfo2(String carrierId, String trackingNumber) throws Exception {
        log.info("{}.fetchTrackingInfo2 Start!", this.getClass().getName());

        String apiUrl = API_BASE_URL + carrierId + "/tracks/" + trackingNumber;
        log.info("외부 배송 조회 API 호출 URL: {}", apiUrl);

        // [핵심 수정] getForEntity의 두 번째 인자로 TrackingDTO.class를 전달하여
        // JSON 응답을 바로 TrackingDTO 객체로 변환하도록 지시합니다.
        ResponseEntity<TrackingDTO> response = restTemplate.getForEntity(apiUrl, TrackingDTO.class);

        // 응답 본문은 이제 바로 TrackingDTO 타입입니다.
        TrackingDTO rDTO = response.getBody();
        log.info("외부 API 응답을 TrackingDTO로 변환 성공.");

        log.info("{}.fetchTrackingInfo2 End!", this.getClass().getName());
        return rDTO;
    }

    @Override
    public TrackingDTO getTrackingInfo(String carrierId, String trackingNumber) throws Exception {
        log.info("스케줄러에 의해 getTrackingInfo가 호출되었습니다. fetchTrackingInfo를 재사용합니다.");

        // 기존에 만든 fetchTrackingInfo 메서드를 그대로 호출하여 결과를 반환합니다.
        // 이렇게 하면 로직을 한 곳에서만 관리할 수 있어 유지보수에 용이합니다.
        return this.fetchTrackingInfo2(carrierId, trackingNumber);
    }
}
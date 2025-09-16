package kopo.poly.service;

import kopo.poly.dto.TrackingDTO;

import java.util.Map;

public interface ITrackingService {
    
    // 비회원 배송 조회 임시
    Map<String, Object> fetchTrackingInfo(String carrierId, String trackingNumber);

    /**
     * 택배사 ID와 운송장 번호로 외부 API를 호출하여 최신 배송 정보를 가져옵니다.
     * @param carrierId 택배사 코드 (예: kr.cjlogistics)
     * @param trackingNumber 운송장 번호
     * @return 파싱된 최신 배송 정보 DTO
     */
    TrackingDTO fetchTrackingInfo2(String carrierId, String trackingNumber) throws Exception;

    /**
     * [새로 추가할 메서드]
     * 스케줄러 등 내부 시스템에서 배송 정보를 조회할 때 사용하는 메서드입니다.
     * 이름만 다를 뿐, 기능은 fetchTrackingInfo와 동일합니다.
     *
     * @param carrierId      택배사 ID
     * @param trackingNumber 운송장 번호
     * @return 외부 API로부터 받은 배송 정보를 담은 TrackingDTO 객체
     * @throws Exception API 호출 및 데이터 처리 중 발생할 수 있는 모든 예외
     */
    TrackingDTO getTrackingInfo(String carrierId, String trackingNumber) throws Exception;
}

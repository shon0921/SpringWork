package kopo.poly.service;

import kopo.poly.dto.PostDTO;

import java.util.List;

public interface IPostService {

    void saveLog(PostDTO dto);

    boolean existsByUserId(String userId, String toAddress, String trackingNumber) throws Exception;

    boolean deleteByUserId(String userId, String toAddress, String trackingNumber);

    List<PostDTO> getLogListByUserId(String userId);

    PostDTO getLogByTrackingInfo(String userId, String toAddress, String trackingNumber);


    /**
     * 아직 배송이 완료되지 않은 모든 배송 로그 목록을 조회합니다.
     * 스케줄러가 이 목록을 가져와 상태를 갱신합니다.
     * @return 배송 미완료 상태인 PostDTO 리스트
     */
    List<PostDTO> getAllUncompletedLogs();

    /**
     * 스케줄러에 의해 감지된 변경 사항을 DB에 업데이트합니다.
     * @param pDTO 업데이트할 내용이 담긴 DTO
     */
    void updateLogByScheduler(PostDTO pDTO) throws Exception;

    void updateFavorite(PostDTO pDTO) throws Exception;
}

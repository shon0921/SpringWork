package kopo.poly.service.impl;

import kopo.poly.dto.PostDTO;
import kopo.poly.persistance.mongodb.impl.PostMapper;
import kopo.poly.service.IPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostService implements IPostService {

    private final PostMapper postMapper;

    /**
     * 새로운 배송 기록을 저장합니다.
     *
     * @param dto 저장할 배송 정보가 담긴 DTO
     */
    @Override
    public void saveLog(PostDTO dto) {
        log.info("{}.saveLog Start!", this.getClass().getName());
        log.info("저장할 배송 정보 -> 사용자: {}, 운송장번호: {}", dto.getUserId(), dto.getTrackingNumber());

        postMapper.insertDeliveryLog(dto);

        log.info("새로운 배송 기록 저장을 완료했습니다.");
        log.info("{}.saveLog End!", this.getClass().getName());
    }

    /**
     * 특정 사용자가 동일한 배송 정보를 이미 저장했는지 확인합니다.
     *
     * @param userId         사용자 ID
     * @param toAddress      받는 사람 주소
     * @param trackingNumber 운송장 번호
     * @return 존재하면 true, 아니면 false
     */
    @Override
    public boolean existsByUserId(String userId, String toAddress, String trackingNumber) {
        log.info("{}.existsByUserId Start!", this.getClass().getName());
        log.info("중복 확인 요청 -> 사용자: {}, 주소: {}, 운송장: {}", userId, toAddress, trackingNumber);

        boolean exists = postMapper.existsByUserId(userId, toAddress, trackingNumber);
        log.info("중복 확인 결과: {}", exists);

        log.info("{}.existsByUserId End!", this.getClass().getName());
        return exists;
    }

    /**
     * 사용자가 저장한 특정 배송 기록 한 건을 삭제합니다.
     *
     * @param userId         사용자 ID
     * @param toAddress      받는 사람 주소
     * @param trackingNumber 운송장 번호
     * @return 삭제 성공 시 true, 실패 시 false
     */
    @Override
    public boolean deleteByUserId(String userId, String toAddress, String trackingNumber) {
        log.info("{}.deleteByUserId Start!", this.getClass().getName());
        log.info("삭제 요청 -> 사용자: {}, 주소: {}, 운송장: {}", userId, toAddress, trackingNumber);

        boolean result = postMapper.deleteByUserId(userId, toAddress, trackingNumber);
        log.info("삭제 처리 결과: {}", result);

        log.info("{}.deleteByUserId End!", this.getClass().getName());
        return result;
    }

    /**
     * 특정 사용자가 저장한 모든 배송 기록 목록을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 배송 기록 DTO 리스트
     */
    @Override
    public List<PostDTO> getLogListByUserId(String userId) {
        log.info("{}.getLogListByUserId Start!", this.getClass().getName());
        log.info("배송 목록 조회 요청 사용자 ID: {}", userId);

        List<PostDTO> logList = postMapper.getLogListByUserId(userId);
        log.info("사용자 '{}'의 배송 기록 {}건을 조회했습니다.", userId, logList.size());

        log.info("{}.getLogListByUserId End!", this.getClass().getName());
        return logList;
    }

    /**
     * 특정 배송 기록 한 건의 상세 정보를 조회합니다.
     *
     * @param userId         사용자 ID
     * @param toAddress      받는 사람 주소
     * @param trackingNumber 운송장 번호
     * @return 조회된 PostDTO, 없으면 null
     */
    @Override
    public PostDTO getLogByTrackingInfo(String userId, String toAddress, String trackingNumber) {
        log.info("{}.getLogByTrackingInfo Start!", this.getClass().getName());
        log.info("단일 배송 정보 조회 요청 -> 사용자: {}, 주소: {}, 운송장: {}", userId, toAddress, trackingNumber);

        PostDTO postDTO = postMapper.getLogByTrackingInfo(userId, toAddress, trackingNumber);
        if (postDTO != null) {
            log.info("단일 배송 정보 조회를 완료했습니다.");
        } else {
            log.warn("해당 조건의 배송 정보를 찾지 못했습니다.");
        }

        log.info("{}.getLogByTrackingInfo End!", this.getClass().getName());
        return postDTO;
    }

    /**
     * 스케줄러가 상태를 갱신할 대상, 즉 '배달완료'가 아닌 모든 배송 기록을 조회합니다.
     *
     * @return 미완료 배송 기록 DTO 리스트
     */
    @Override
    public List<PostDTO> getAllUncompletedLogs() {
        log.info("{}.getAllUncompletedLogs Start!", this.getClass().getName());

        // '배송완료' 상태가 아닌 모든 로그를 가져옵니다.
        // 이 상태 텍스트는 Mapper의 실제 구현과 일치해야 합니다. (현재는 stateId='delivered' 기준)
        String completedStateText = "배송완료"; // 이 변수는 현재 Mapper 구현에서는 사용되지 않음
        log.info("Mapper를 호출하여 미완료된 모든 배송 기록 조회를 시작합니다.");

        List<PostDTO> result = postMapper.getAllUncompletedLogs(completedStateText);
        log.info("조회된 미완료 배송 건수: {}건", result.size());

        log.info("{}.getAllUncompletedLogs End!", this.getClass().getName());
        return result;
    }

    /**
     * 스케줄러에 의해 감지된 변경 사항을 DB에 업데이트합니다.
     *
     * @param pDTO 업데이트할 필드 값만 담고 있는 DTO
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public void updateLogByScheduler(PostDTO pDTO) throws Exception {
        log.info("{}.updateLogByScheduler Start!", this.getClass().getName());
        log.info("스케줄러 업데이트 요청 -> 사용자: {}, 운송장: {}", pDTO.getUserId(), pDTO.getTrackingNumber());

        postMapper.updateLogByScheduler(pDTO);

        log.info("스케줄러를 통한 배송 정보 업데이트를 완료했습니다.");
        log.info("{}.updateLogByScheduler End!", this.getClass().getName());
    }

    /**
     * 즐겨찾기 상태를 DB에서 업데이트합니다.
     *
     * @param dto toAddress, trackingNumber, Favorite, userId 포함
     */
    @Override
    public void updateFavorite(PostDTO dto) throws Exception {
        log.info("{}.updateFavorite Start! -> 사용자: {}, 운송장: {}, 즐겨찾기: {}",
                this.getClass().getName(),
                dto != null ? dto.getUserId() : "NULL",
                dto != null ? dto.getTrackingNumber() : "NULL",
                dto != null ? dto.getFavorites() : "NULL");

        if (dto == null) {
            log.warn("DTO가 null입니다. 작업을 종료합니다.");
            return;
        }

        try {
            postMapper.updateFavorite(dto);
            log.info("즐겨찾기 상태 업데이트 완료 -> 사용자: {}, 운송장: {}, 즐겨찾기: {}",
                    dto.getUserId(), dto.getTrackingNumber(), dto.getFavorites());
        } catch (Exception e) {
            log.error("즐겨찾기 업데이트 중 오류 발생 -> 사용자: {}, 운송장: {}",
                    dto.getUserId(), dto.getTrackingNumber(), e);
            throw e;
        }

        log.info("{}.updateFavorite End!", this.getClass().getName());
    }
}

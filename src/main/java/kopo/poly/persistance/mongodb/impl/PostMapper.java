package kopo.poly.persistance.mongodb.impl;

import com.mongodb.MongoException;
import com.mongodb.client.result.DeleteResult;
import kopo.poly.dto.PostDTO;
import kopo.poly.dto.UserDTO;
import kopo.poly.persistance.mongodb.IPostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostMapper implements IPostMapper {

    private final MongoTemplate mongodb;
    private static final String colNm = "POST";

    /**
     * 새로운 배송 기록을 DB에 저장합니다.
     * 알림 발송 여부 플래그도 함께 저장됩니다.
     *
     * @param dto 프론트엔드에서 전달된 저장할 배송 정보 DTO
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void insertDeliveryLog(PostDTO dto) throws MongoException {
        log.info("{}.insertDeliveryLog Start!", this.getClass().getName());
        log.info("저장할 배송 정보 -> 사용자: {}, 운송장번호: {}", dto.getUserId(), dto.getTrackingNumber());

        Document doc = new Document()
                .append("carrierId", dto.getCarrierId())
                .append("carrierName", dto.getCarrierName())
                .append("fromName", dto.getFromName())
                .append("toName", dto.getToName())
                .append("toAddress", dto.getToAddress())
                .append("stateId", dto.getStateId())
                .append("stateText", dto.getStateText())
                .append("userId", dto.getUserId())
                .append("regDt", dto.getRegDt())
                .append("trackingNumber", dto.getTrackingNumber())
                .append("lastDeliveryTime", dto.getLastDeliveryTime())
                .append("startAddress", dto.getStartAddress())
                .append("startLat", dto.getStartLat())
                .append("startLng", dto.getStartLng())
                // 알림 발송 여부 플래그 저장 (기본값은 false)
                .append("notificationSentForOutForDelivery", dto.isNotificationSentForOutForDelivery())
                .append("notificationSentForDelivered", dto.isNotificationSentForDelivered())
                .append("favorites", dto.getFavorites());

        mongodb.getCollection(colNm).insertOne(doc);

        log.info("새로운 배송 정보(운송장번호: {}) 저장을 완료했습니다.", dto.getTrackingNumber());
        log.info("{}.insertDeliveryLog End!", this.getClass().getName());
    }

    /**
     * 특정 사용자가 동일한 배송 정보를 이미 저장했는지 확인합니다.
     *
     * @param userId         사용자 ID
     * @param toAddress      받는 사람 주소
     * @param trackingNumber 운송장 번호
     * @return 존재하면 true, 아니면 false
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public boolean existsByUserId(String userId, String toAddress, String trackingNumber) throws MongoException {
        log.info("{}.existsByUserId Start!", this.getClass().getName());
        log.info("중복 확인 -> 사용자: {}, 주소: {}, 운송장: {}", userId, toAddress, trackingNumber);

        Query query = Query.query(Criteria.where("userId").is(userId)
                .and("toAddress").is(toAddress)
                .and("trackingNumber").is(trackingNumber));
        boolean exists = mongodb.exists(query, PostDTO.class, colNm);

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
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public boolean deleteByUserId(String userId, String toAddress, String trackingNumber) throws MongoException {
        log.info("{}.deleteByUserId Start!", this.getClass().getName());
        log.info("삭제할 배송 기록 -> 사용자: {}, 주소: {}, 운송장: {}", userId, toAddress, trackingNumber);

        Query query = Query.query(Criteria.where("userId").is(userId)
                .and("toAddress").is(toAddress)
                .and("trackingNumber").is(trackingNumber));
        DeleteResult result = mongodb.remove(query, PostDTO.class, colNm);

        log.info("삭제 결과: {}건 삭제됨", result.getDeletedCount());
        log.info("{}.deleteByUserId End!", this.getClass().getName());
        return result.getDeletedCount() > 0;
    }

    /**
     * 특정 사용자가 저장한 모든 배송 기록 목록을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 배송 기록 DTO 리스트
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public List<PostDTO> getLogListByUserId(String userId) throws MongoException {
        log.info("{}.getLogListByUserId Start!", this.getClass().getName());
        log.info("배송 목록을 조회할 사용자 ID: {}", userId);

        Query query = Query.query(Criteria.where("userId").is(userId));

        // 정렬 조건 변경: 1. 즐겨찾기 우선, 2. 최신순
        Sort sort = Sort.by(Sort.Direction.DESC, "favorites")
                .and(Sort.by(Sort.Direction.DESC, "regDt"));

        query.with(sort); // 새로운 정렬 조건 적용

        List<PostDTO> result = mongodb.find(query, PostDTO.class, colNm);

        log.info("사용자 '{}'의 배송 기록 {}건을 조회했습니다.", userId, result.size());
        log.info("{}.getLogListByUserId End!", this.getClass().getName());
        return result;
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
        log.info("단일 배송 정보 조회 -> 사용자: {}, 주소: {}, 운송장: {}", userId, toAddress, trackingNumber);

        Query query = Query.query(Criteria.where("userId").is(userId)
                .and("toAddress").is(toAddress)
                .and("trackingNumber").is(trackingNumber));
        PostDTO rDTO = mongodb.findOne(query, PostDTO.class, colNm);

        if (rDTO == null) {
            log.warn("해당 조건의 배송 정보를 찾을 수 없습니다.");
        } else {
            log.info("단일 배송 정보 조회를 완료했습니다.");
        }
        log.info("{}.getLogByTrackingInfo End!", this.getClass().getName());
        return rDTO;
    }

    /**
     * 스케줄러가 상태를 갱신할 대상, 즉 '배달완료'가 아닌 모든 배송 기록을 조회합니다.
     *
     * @param completedStateText 현재 로직에서는 사용되지 않음 (향후 확장성을 위해 유지)
     * @return 미완료 배송 기록 DTO 리스트
     */
    @Override
    public List<PostDTO> getAllUncompletedLogs(String completedStateText) {
        log.info("{}.getAllUncompletedLogs Start!", this.getClass().getName());

        // 조회 조건: stateId가 'delivered'가 아니거나, 필드가 존재하지 않는 모든 문서
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("stateId").ne("delivered"),
                Criteria.where("stateId").exists(false)
        );
        log.info("조회 조건: stateId가 'delivered'가 아닌 모든 문서");
        Query query = new Query(criteria);
        List<PostDTO> result = mongodb.find(query, PostDTO.class, colNm);

        log.info("조회된 미완료 배송 건수: {}건", result.size());
        log.info("{}.getAllUncompletedLogs End!", this.getClass().getName());
        return result;
    }

    /**
     * 스케줄러에 의해 감지된 변경 사항을 DB에 업데이트합니다. (부분 업데이트)
     *
     * @param pDTO 업데이트할 필드 값만 담고 있는 DTO
     */
    @Override
    public void updateLogByScheduler(PostDTO pDTO) {
        log.info("{}.updateLogByScheduler Start!", this.getClass().getName());
        log.info("업데이트할 정보 -> 사용자: {}, 운송장번호: {}", pDTO.getUserId(), pDTO.getTrackingNumber());

        Criteria criteria = Criteria.where("userId").is(pDTO.getUserId())
                .and("trackingNumber").is(pDTO.getTrackingNumber());
        Query query = new Query(criteria);

        // 업데이트할 필드들을 동적으로 설정 ($set)
        Update update = new Update();
        if (pDTO.getStateId() != null) update.set("stateId", pDTO.getStateId());
        if (pDTO.getStateText() != null) update.set("stateText", pDTO.getStateText());
        if (pDTO.getLastDeliveryTime() != null) update.set("lastDeliveryTime", pDTO.getLastDeliveryTime());

        // 알림 플래그가 true('Y')로 설정된 경우에만 DB에 업데이트
        if (pDTO.isNotificationSentForOutForDelivery()) {
            update.set("notificationSentForOutForDelivery", true);
        }
        if (pDTO.isNotificationSentForDelivered()) {
            update.set("notificationSentForDelivered", true);
        }

        mongodb.updateFirst(query, update, colNm);

        log.info("사용자 ID: {}, 운송장번호: {} 로그 업데이트를 완료했습니다.", pDTO.getUserId(), pDTO.getTrackingNumber());
        log.info("{}.updateLogByScheduler End!", this.getClass().getName());
    }

    /**
     * 특정 사용자가 저장한 *모든* 배송 기록을 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param pDTO 사용자 정보 (userId 필요)
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void deletePostByUserId(UserDTO pDTO) throws MongoException {
        log.info("{}.deletePostByUserId Start!", this.getClass().getName());
        log.info("모든 배송 기록을 삭제할 사용자 ID: {}", pDTO.getUserId());

        Query query = Query.query(Criteria.where("userId").is(pDTO.getUserId()));
        DeleteResult result = mongodb.remove(query, colNm);

        log.info("'{}' 회원의 배송조회 기록 {}개가 삭제되었습니다.", pDTO.getUserId(), result.getDeletedCount());
        log.info("{}.deletePostByUserId End!", this.getClass().getName());
    }

    @Override
    public void updateFavorite(PostDTO dto) {
        log.info("{}.updateFavorite Start!", this.getClass().getName());
        log.info("즐겨찾기 상태 업데이트 -> 사용자: {}, 주소: {}, 운송장: {}, 새 상태: {}",
                dto.getUserId(), dto.getToAddress(), dto.getTrackingNumber(), dto.getFavorites());

        Query query = Query.query(Criteria.where("userId").is(dto.getUserId())
                .and("toAddress").is(dto.getToAddress())
                .and("trackingNumber").is(dto.getTrackingNumber()));

        Update update = new Update().set("favorites", dto.getFavorites());

        mongodb.updateFirst(query, update, colNm);

        log.info("즐겨찾기 상태 업데이트 완료 -> 사용자: {}, 운송장: {}", dto.getUserId(), dto.getTrackingNumber());
        log.info("{}.updateFavorite End!", this.getClass().getName());
    }
}
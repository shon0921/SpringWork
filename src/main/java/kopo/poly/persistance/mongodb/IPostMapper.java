package kopo.poly.persistance.mongodb;

import com.mongodb.MongoException;
import kopo.poly.dto.PostDTO;
import kopo.poly.dto.UserDTO;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IPostMapper {

    // 배송조회 저장
    void insertDeliveryLog(PostDTO dto) throws Exception;

    // 배송조회에 저장되어 있는지 확인
    boolean existsByUserId(String userId, String toAddress, String trackingNumber) throws Exception;

    // 배송조회 삭제
    boolean deleteByUserId(String userId, String toAddress, String trackingNumber) throws Exception;

    List<PostDTO> getLogListByUserId(String userId) throws Exception;

    PostDTO getLogByTrackingInfo(@Param("userId") String userId, @Param("toAddress") String toAddress, @Param("trackingNumber") String trackingNumber);

    /**
     * 특정 상태가 아닌 모든 배송 로그를 조회하는 쿼리를 호출합니다.
     * @param completedStateText 제외할 상태 텍스트 (예: "배송완료")
     * @return 배송 미완료 상태인 PostDTO 리스트
     */
    List<PostDTO> getAllUncompletedLogs(@Param("completedStateText") String completedStateText);

    /**
     * 스케줄러가 전달한 정보로 배송 로그를 업데이트하는 쿼리를 호출합니다.
     * 식별자로는 USER_ID와 TRACKING_NUMBER를 사용합니다.
     * @param pDTO 업데이트할 정보가 담긴 DTO
     */
    void updateLogByScheduler(PostDTO pDTO);

    // 회원 탈퇴시 배송내역 전부 삭제
    void deletePostByUserId(UserDTO pDTO) throws MongoException;


    // 즐겨찾기 상태 업데이트
    void updateFavorite(PostDTO dto);
}

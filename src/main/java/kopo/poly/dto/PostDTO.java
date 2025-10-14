package kopo.poly.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
public class PostDTO {

    private String userId;       // 저장 아이디
    private String carrierName; // 택배사
    private String fromName;    // 보내는사람
    private String toName;      // 받는사람
    private String stateText;   // 최근 배송 상태
    private String toAddress;   // 주소
    private String regDt;       // 최근 갱신 날짜
    private String trackingNumber; // 송장번호
    private String lastDeliveryTime;    // 배송 내 시간
    private String carrierId;
    private String startAddress;    // 출발지 주소
    private Double startLat;    // 위도 값
    private Double startLng;    // 경도 값

    private String chgDt;
    private String stateId; // 상태

    // [추가] 알림 발송 여부 플래그
    private boolean notificationSentForOutForDelivery; // '배달전' 알림 발송 여부
    private boolean notificationSentForDelivered;      // '배달완료' 알림 발송 여부

    private String favorites = "N";   // 택배 북마크 기본값 N
}

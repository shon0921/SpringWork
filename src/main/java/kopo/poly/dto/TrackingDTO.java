package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
// API 응답에 있지만 이 DTO 클래스에는 정의되지 않은 필드가 있어도 오류 없이 무시합니다.
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackingDTO {

    // 최상위 JSON 필드에 해당하는 멤버 변수들
    private FromToInfo from;
    private FromToInfo to;
    private StateInfo state;
    private List<ProgressInfo> progresses;
    private CarrierInfo carrier;

    // --- 아래는 JSON 내부의 중첩된 객체들을 표현하기 위한 정적 내부 클래스(Static Inner Class)들입니다. ---

    /**
     * 보내는 사람(from)과 받는 사람(to) 정보를 담는 DTO
     */
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FromToInfo {
        private String name;
        private String time;
    }

    /**
     * 현재 배송 상태(state) 정보를 담는 DTO
     */
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StateInfo {
        private String id;
        private String text;
    }

    /**
     * 배송 진행 과정(progresses)의 각 단계를 담는 DTO
     */
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProgressInfo {
        private String time;
        private LocationInfo location;
        private StatusInfo status;
        private String description;
    }

    /**
     * 위치(location) 정보를 담는 DTO
     */
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocationInfo {
        private String name;
    }

    /**
     * 단계별 상태(status) 정보를 담는 DTO
     */
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusInfo {
        private String id;
        private String text;
    }

    /**
     * 택배사(carrier) 정보를 담는 DTO
     */
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CarrierInfo {
        private String id;
        private String name;
        private String tel;
    }
}
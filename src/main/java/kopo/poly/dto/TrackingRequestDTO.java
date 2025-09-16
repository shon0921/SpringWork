package kopo.poly.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackingRequestDTO {

    @NotBlank(message = "택배사를 선택해주세요.")
    private String carrierId;

    @NotBlank(message = "운송장 번호를 입력해주세요.")
    private String trackingNumber;

    @NotBlank(message = "도착 주소를 입력해주세요.")
    private String addr1;
}

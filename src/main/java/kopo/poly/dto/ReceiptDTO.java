package kopo.poly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptDTO {

    private String receiptSeq;  // 영수증 순번
    private String userId;  // 닉네임
    private String Money;   // 결제금액
    private String Date;    // 날짜
}

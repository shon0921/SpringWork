package kopo.poly.persistance.mongodb;

import kopo.poly.dto.ReceiptDTO;

public interface IReceiptMapper {

    /**
     * 영수증 정보 저장
     *
     * @param pDTO 저장할 정보
     * @param colNm 저장할 컬렉션 이름
     * @return 저장 결과
     */
    ReceiptDTO insertReceipt(ReceiptDTO pDTO) throws Exception;

}
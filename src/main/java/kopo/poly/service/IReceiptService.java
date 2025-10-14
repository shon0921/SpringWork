package kopo.poly.service;

import kopo.poly.dto.ReceiptDTO;

public interface IReceiptService {

    /**
     * 영수증 정보 DB 저장
     *
     * @return
     */
    ReceiptDTO insertReceipt(ReceiptDTO pDTO) throws Exception;
}

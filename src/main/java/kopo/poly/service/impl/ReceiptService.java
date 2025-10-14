package kopo.poly.service.impl;

import kopo.poly.dto.ReceiptDTO;
import kopo.poly.persistance.mongodb.IReceiptMapper;
import kopo.poly.service.IReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService implements IReceiptService {

    private final IReceiptMapper receiptMapper;

    @Override
    public ReceiptDTO insertReceipt(ReceiptDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".insertReceipt Start!");

        // 영수증 정보를 MongoDB에 저장
        receiptMapper.insertReceipt(pDTO); // "ReceiptCollection"은 컬렉션 이름

        log.info(this.getClass().getName() + ".insertReceipt End!");
        return pDTO;
    }
}
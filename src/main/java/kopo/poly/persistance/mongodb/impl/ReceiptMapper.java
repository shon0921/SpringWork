package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import kopo.poly.dto.ReceiptDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IReceiptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptMapper extends AbstractMongoDBComon implements IReceiptMapper {

    private final MongoTemplate mongodb;

    /**
     * MongoDB의 counters 컬렉션을 사용하여 지정된 시퀀스 이름의 값을 1 증가시키고 반환합니다.
     * (DB의 AUTO_INCREMENT와 유사한 기능 구현)
     */
    private String getNextSequence(String sequenceName) {
        MongoCollection<Document> counterCol = mongodb.getCollection("counters3"); // 자동증분 기능

        Document filter = new Document("_id", sequenceName);
        Document update = new Document("$inc", new Document("seq", 1L)); // long 타입으로 증가
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER) // 업데이트 후의 문서를 반환
                .upsert(true); // 시퀀스가 없으면 새로 생성

        Document updatedDoc = counterCol.findOneAndUpdate(filter, update, options);

        // updatedDoc이 null이 아닐 경우 seq 값을, null이면 1을 반환 (최초 생성 시)
        if (updatedDoc != null && updatedDoc.get("seq") != null) {
            return String.valueOf(updatedDoc.getLong("seq"));
        } else {
            return "1";
        }
    }

    /**
     * 영수증 정보를 DB에 저장하고, 생성된 순번이 포함된 DTO를 반환합니다.
     */
    @Override
    public ReceiptDTO insertReceipt(ReceiptDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".insertReceipt Start!");

        String colNm = "ReceiptCollection"; // 저장할 컬렉션 이름

        // --- 🚨 1. getNextSequence 메서드를 호출하도록 수정 ---
        // 시퀀스 이름으로 "receiptSeq"를 사용하여 순번을 가져옵니다.
        String nextSeq = this.getNextSequence("receiptSeq");
        log.info("Generated Receipt Seq: {}", nextSeq);

        // --- 🚨 2. DTO의 receiptSeq 필드에 가져온 순번을 저장합니다 ---
        pDTO.setReceiptSeq(nextSeq);

        // DTO를 Document(BSON)으로 변환
        MongoCollection<Document> col = mongodb.getCollection(colNm);
        Document doc = new Document(new ObjectMapper().convertValue(pDTO, Map.class));

        // DB에 저장
        col.insertOne(doc);

        log.info(this.getClass().getName() + ".insertReceipt End!");

        // 3. seq가 저장된 DTO를 그대로 서비스에 반환합니다.
        return pDTO;
    }
}
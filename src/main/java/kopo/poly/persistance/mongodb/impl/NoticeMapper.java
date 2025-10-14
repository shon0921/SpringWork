package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import kopo.poly.dto.NoticeDTO;
import kopo.poly.persistance.mongodb.INoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.events.MappingEndEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class NoticeMapper implements INoticeMapper {

    private final MongoTemplate mongodb;

    private static final String colNM = "NOTICE";
    private static final String LIKE_COL = "NOTICE_LIKE";

    /**
     * MongoDB의 counters 컬렉션을 사용하여 지정된 시퀀스 이름의 값을 1 증가시키고 반환합니다.
     * (DB의 AUTO_INCREMENT와 유사한 기능 구현)
     *
     * @param sequenceName 시퀀스로 사용할 이름 (e.g., "noticeseq")
     * @return 증가된 시퀀스 값 (문자열)
     */
    private String getNextSequence(String sequenceName) {
        MongoCollection<Document> counterCol = mongodb.getCollection("counters");

        Document filter = new Document("_id", sequenceName);
        Document update = new Document("$inc", new Document("seq", 1));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER) // 업데이트 후의 문서를 반환
                .upsert(true); // 시퀀스가 없으면 새로 생성

        Document updatedDoc = counterCol.findOneAndUpdate(filter, update, options);
        return String.valueOf(updatedDoc.get("seq"));
    }

    /**
     * 모든 공지사항 목록을 조회합니다.
     *
     * @return 조회된 공지사항 DTO 리스트
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public List<NoticeDTO> getNoticeList() throws MongoException {
        log.info("{}.getNoticeList Start!", this.getClass().getName());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        List<NoticeDTO> noticeList = new ArrayList<>();

        // [수정] 로그 메시지에서 '공지여부' 정렬 부분을 삭제합니다.
        log.info("전체 공지사항 목록 조회를 시작합니다. (정렬: 등록일 DESC - 최신순)");

        // [수정] 정렬 기준에서 noticeYn을 제거하고, regDt 내림차순(최신순)만 남깁니다.
        FindIterable<Document> documents = col.find()
                .projection(Projections.exclude("_id"))
                .sort(Sorts.descending("regDt"));

        // 조회된 각 Document를 NoticeDTO로 변환하여 리스트에 추가
        for (Document doc : documents) {
            NoticeDTO dto = new NoticeDTO();
            dto.setNoticeSeq(doc.getString("noticeSeq"));
            dto.setTitle(doc.getString("title"));
            dto.setNoticeYn(doc.getString("noticeYn"));
            dto.setContents(doc.getString("contents"));
            dto.setUserId(doc.getString("userId"));
            dto.setRegDt(doc.getString("regDt"));
            dto.setChgDt(doc.getString("chgDt"));

            // readCnt는 숫자 타입이므로 안전하게 변환
            Object readCnt = doc.get("readCnt");
            dto.setReadCnt(readCnt != null ? ((Number) readCnt).intValue() : 0);
            noticeList.add(dto);
        }

        log.info("총 {}건의 공지사항을 조회했습니다.", noticeList.size());
        log.info("{}.getNoticeList End!", this.getClass().getName());
        return noticeList;
    }

    /**
     * 새로운 공지사항을 DB에 삽입합니다.
     *
     * @param pDTO 삽입할 공지사항 정보를 담은 DTO
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void insertNoticeInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.insertNoticeInfo Start!", this.getClass().getName());
        log.info("작성자: {}, 제목: {}", pDTO.getUserId(), pDTO.getTitle());

        MongoCollection<Document> col = mongodb.getCollection(colNM);

        // noticeSeq: 자동 증가 시퀀스로 설정
        String nextSeq = getNextSequence("noticeseq");
        pDTO.setNoticeSeq(nextSeq);
        log.info("생성된 공지사항 시퀀스: {}", nextSeq);

        // 기본 필드 설정
        pDTO.setReadCnt(0);
        pDTO.setRegDt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        pDTO.setChgDt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Document 생성 및 MongoDB에 삽입
        Document doc = new Document("noticeSeq", pDTO.getNoticeSeq())
                .append("title", pDTO.getTitle())
                .append("noticeYn", pDTO.getNoticeYn())
                .append("contents", pDTO.getContents())
                .append("readCnt", pDTO.getReadCnt())
                .append("userId", pDTO.getUserId())
                .append("regDt", pDTO.getRegDt())
                .append("chgDt", pDTO.getChgDt())
                .append("like",pDTO.getLike());
        col.insertOne(doc);

        log.info("공지사항(시퀀스: {}) 삽입을 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.insertNoticeInfo End!", this.getClass().getName());
    }

    /**
     * 특정 공지사항의 상세 정보를 조회합니다.
     *
     * @param pDTO 조회할 공지사항의 noticeSeq가 포함된 DTO
     * @return 조회된 공지사항 DTO, 없는 경우 null
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public NoticeDTO getNoticeInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.getNoticeInfo Start!", this.getClass().getName());
        log.info("조회할 공지사항 시퀀스: {}", pDTO.getNoticeSeq());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document query = new Document("noticeSeq", pDTO.getNoticeSeq());
        Document doc = col.find(query).first();

        // Document를 DTO로 변환 (ObjectMapper 사용)
        NoticeDTO rDTO = (doc != null) ? new ObjectMapper().convertValue(doc, NoticeDTO.class) : null;

        if (rDTO == null) {
            log.warn("시퀀스 '{}'에 해당하는 공지사항을 찾을 수 없습니다.", pDTO.getNoticeSeq());
        } else {
            log.info("시퀀스 '{}'에 해당하는 공지사항을 성공적으로 조회했습니다.", pDTO.getNoticeSeq());
        }

        log.info("{}.getNoticeInfo End!", this.getClass().getName());
        return rDTO;
    }

    /**
     * 특정 공지사항의 조회수를 1 증가시킵니다.
     *
     * @param pDTO 조회수를 증가시킬 공지사항의 noticeSeq가 포함된 DTO
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void updateNoticeReadCnt(NoticeDTO pDTO) throws MongoException {
        log.info("{}.updateNoticeReadCnt Start!", this.getClass().getName());
        log.info("조회수를 증가시킬 공지사항 시퀀스: {}", pDTO.getNoticeSeq());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document query = new Document("noticeSeq", pDTO.getNoticeSeq());
        Document update = new Document("$inc", new Document("readCnt", 1)); // readCnt 필드 1 증가
        col.updateOne(query, update);

        log.info("공지사항(시퀀스: {}) 조회수 업데이트를 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.updateNoticeReadCnt End!", this.getClass().getName());
    }

    /**
     * 특정 공지사항의 정보를 수정합니다.
     *
     * @param pDTO 수정할 정보가 담긴 DTO (noticeSeq, title, contents 등)
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void updateNoticeInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.updateNoticeInfo Start!", this.getClass().getName());
        log.info("수정할 공지사항 시퀀스: {}", pDTO.getNoticeSeq());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document query = new Document("noticeSeq", pDTO.getNoticeSeq());
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // $set 연산자를 사용하여 특정 필드만 업데이트
        Document updateFields = new Document("title", pDTO.getTitle())
                .append("noticeYn", pDTO.getNoticeYn())
                .append("contents", pDTO.getContents())
                .append("chgDt", currentDateTime)
                .append("userId", pDTO.getUserId());
        Document update = new Document("$set", updateFields);
        col.updateOne(query, update);

        log.info("공지사항(시퀀스: {}) 정보 수정을 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.updateNoticeInfo End!", this.getClass().getName());
    }

    /**
     * 특정 공지사항을 삭제합니다.
     *
     * @param pDTO 삭제할 공지사항의 noticeSeq가 포함된 DTO
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void deleteNoticeInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.deleteNoticeInfo Start!", this.getClass().getName());
        log.info("삭제할 공지사항 시퀀스: {}", pDTO.getNoticeSeq());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document query = new Document("noticeSeq", pDTO.getNoticeSeq());
        col.deleteOne(query);

        log.info("공지사항(시퀀스: {}) 삭제를 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.deleteNoticeInfo End!", this.getClass().getName());
    }

    @Override
    public void updateNoticeLike(NoticeDTO pDTO) throws MongoException{
        MongoCollection<Document> likeCol = mongodb.getCollection(LIKE_COL);
        MongoCollection<Document> noticeCol = mongodb.getCollection(colNM);

        Document query = new Document("noticeSeq", pDTO.getNoticeSeq())
                .append("userId", pDTO.getUserId());

        if (likeCol.find(query).first() == null) {
            // 좋아요 기록이 없으면 추가
            likeCol.insertOne(query);
            // 공지사항 컬렉션 like 필드 +1
            noticeCol.updateOne(Filters.eq("noticeSeq", pDTO.getNoticeSeq()),
                    Updates.inc("like", 1));
        }
    }

    @Override
    public void cancelNoticeLike(NoticeDTO pDTO) throws MongoException {
        MongoCollection<Document> likeCol = mongodb.getCollection(LIKE_COL);
        MongoCollection<Document> noticeCol = mongodb.getCollection(colNM);

        Document query = new Document("noticeSeq", pDTO.getNoticeSeq())
                .append("userId", pDTO.getUserId());

        if (likeCol.find(query).first() != null) {
            // 좋아요 기록이 있으면 삭제
            likeCol.deleteOne(query);
            // 공지사항 컬렉션 like 필드 -1
            noticeCol.updateOne(Filters.eq("noticeSeq", pDTO.getNoticeSeq()),
                    Updates.inc("like", -1));
        }
    }

    @Override
    public int getNoticeLikeCount(String noticeSeq) throws MongoException {
        MongoCollection<Document> noticeCol = mongodb.getCollection(colNM);
        Document doc = noticeCol.find(Filters.eq("noticeSeq", noticeSeq))
                .projection(new Document("like", 1).append("_id", 0))
                .first();
        if (doc == null || doc.get("like") == null) return 0;
        return ((Number) doc.get("like")).intValue();
    }

    @Override
    public boolean isNoticeLikedByUser(String noticeSeq, String userId) throws MongoException {
        MongoCollection<Document> likeCol = mongodb.getCollection(LIKE_COL);
        Document query = new Document("noticeSeq", noticeSeq).append("userId", userId);
        return likeCol.find(query).first() != null;
    }
}

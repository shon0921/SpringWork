package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import kopo.poly.dto.NoticeDTO;
import kopo.poly.dto.UserDTO;
import kopo.poly.persistance.mongodb.ICommentMapper;
import kopo.poly.persistance.mongodb.IInquiryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component("InquiryMapper")
public class InquiryMapper implements IInquiryMapper {

    private final MongoTemplate mongodb;
    private final ICommentMapper commentMapper; // 다른 맵퍼(댓글)와 연동

    private static final String colNM = "INQUIRY";

    /**
     * MongoDB의 counters 컬렉션을 사용하여 지정된 시퀀스 이름의 값을 1 증가시키고 반환합니다.
     */
    private String getNextSequence(String sequenceName) {
        MongoCollection<Document> counterCol = mongodb.getCollection("counters2");
        Document filter = new Document("_id", sequenceName);
        Document update = new Document("$inc", new Document("seq", 1));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER)
                .upsert(true);
        Document updatedDoc = counterCol.findOneAndUpdate(filter, update, options);
        return String.valueOf(updatedDoc.get("seq"));
    }

    /**
     * 문의글 목록을 조회합니다.
     */
    @Override
    public List<NoticeDTO> getInquiryList(NoticeDTO pDTO) throws MongoException {
        log.info("{}.getInquiryList Start!", this.getClass().getName());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        List<NoticeDTO> noticeList = new ArrayList<>();
        Document filter = new Document();

        if (pDTO != null && pDTO.getUserId() != null && !pDTO.getUserId().isEmpty()) {
            log.info("사용자 '{}'의 문의글 목록을 조회합니다.", pDTO.getUserId());
            filter.append("userId", pDTO.getUserId());
        } else {
            log.info("관리자 권한으로 전체 문의글 목록을 조회합니다.");
        }

        FindIterable<Document> documents = col.find(filter)
                .projection(Projections.exclude("_id"))
                .sort(Sorts.descending("regDt"));

        for (Document doc : documents) {
            NoticeDTO dto = new NoticeDTO();
            dto.setNoticeSeq(doc.getString("noticeSeq"));
            dto.setTitle(doc.getString("title"));
            dto.setNoticeYn(doc.getString("noticeYn"));
            dto.setContents(doc.getString("contents"));
            dto.setUserId(doc.getString("userId"));
            dto.setRegDt(doc.getString("regDt"));
            dto.setChgDt(doc.getString("chgDt"));
            // ✨ [수정] imageUrl 필드 조회 추가
            dto.setImageUrl(doc.getString("imageUrl"));

            Object readCnt = doc.get("readCnt");
            dto.setReadCnt(readCnt != null ? ((Number) readCnt).intValue() : 0);
            noticeList.add(dto);
        }

        log.info("총 {}건의 문의글을 조회했습니다.", noticeList.size());
        log.info("{}.getInquiryList End!", this.getClass().getName());
        return noticeList;
    }

    /**
     * 새로운 문의글을 DB에 삽입합니다.
     */
    @Override
    public void insertInquiryInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.insertInquiryInfo Start!", this.getClass().getName());
        log.info("작성자: {}, 제목: {}", pDTO.getUserId(), pDTO.getTitle());

        MongoCollection<Document> col = mongodb.getCollection(colNM);

        String nextSeq = getNextSequence("noticeseq");
        pDTO.setNoticeSeq(nextSeq);
        log.info("생성된 문의글 시퀀스: {}", nextSeq);

        pDTO.setReadCnt(0);
        pDTO.setRegDt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        pDTO.setChgDt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // ✨ [수정] Document를 생성할 때 imageUrl 필드 추가
        Document doc = new Document("noticeSeq", pDTO.getNoticeSeq())
                .append("title", pDTO.getTitle())
                .append("noticeYn", pDTO.getNoticeYn())
                .append("contents", pDTO.getContents())
                .append("readCnt", pDTO.getReadCnt())
                .append("userId", pDTO.getUserId())
                .append("regDt", pDTO.getRegDt())
                .append("chgDt", pDTO.getChgDt())
                .append("imageUrl", pDTO.getImageUrl()); // 이미지 URL 추가
        col.insertOne(doc);

        log.info("문의글(시퀀스: {}) 삽입을 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.insertInquiryInfo End!", this.getClass().getName());
    }

    /**
     * 특정 문의글의 상세 정보를 조회합니다.
     */
    @Override
    public NoticeDTO getInquiryInfo(NoticeDTO pDTO, boolean type) throws MongoException {
        log.info("{}.getInquiryInfo Start!", this.getClass().getName());
        log.info("조회할 문의글 시퀀스: {}", pDTO.getNoticeSeq());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document query = new Document("noticeSeq", pDTO.getNoticeSeq());
        Document doc = col.find(query).first();

        if (doc == null) {
            log.warn("시퀀스 '{}'에 해당하는 문의글을 찾을 수 없습니다.", pDTO.getNoticeSeq());
            return null;
        }

        if (type) {
            log.info("조회수 증가 로직 실행");
            Bson update = Updates.inc("readCnt", 1);
            col.updateOne(query, update);
        }

        // ✨ [수정] ObjectMapper를 사용하여 Document를 DTO로 변환하도록 변경 (imageUrl 자동 매핑)
        NoticeDTO rDTO = new ObjectMapper().convertValue(doc, NoticeDTO.class);

        // 조회수 증가가 있었다면, DTO에도 반영
        if(type) {
            rDTO.setReadCnt(rDTO.getReadCnt() + 1);
        }

        log.info("시퀀스 '{}'에 해당하는 문의글을 성공적으로 조회했습니다.", pDTO.getNoticeSeq());
        log.info("{}.getInquiryInfo End!", this.getClass().getName());
        return rDTO;
    }


    /**
     * 특정 문의글의 정보를 수정합니다.
     */
    @Override
    public void updateInquiryInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.updateInquiryInfo Start!", this.getClass().getName());
        log.info("수정할 문의글 시퀀스: {}", pDTO.getNoticeSeq());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document query = new Document("noticeSeq", pDTO.getNoticeSeq());
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Document updateFields = new Document();
        updateFields.append("title", pDTO.getTitle());
        updateFields.append("contents", pDTO.getContents());
        updateFields.append("chgDt", currentDateTime);
        updateFields.append("userId", pDTO.getUserId());

        // ✨ [수정] imageUrl이 있는 경우에만 업데이트 필드에 추가
        if (pDTO.getImageUrl() != null && !pDTO.getImageUrl().isEmpty()) {
            updateFields.append("imageUrl", pDTO.getImageUrl());
            log.info("이미지 URL을 업데이트합니다: {}", pDTO.getImageUrl());
        }

        Document update = new Document("$set", updateFields);
        col.updateOne(query, update);

        log.info("문의글(시퀀스: {}) 정보 수정을 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.updateInquiryInfo End!", this.getClass().getName());
    }

    /**
     * 특정 문의글과 그에 딸린 모든 댓글을 삭제합니다.
     */
    @Override
    public void deleteInquiryInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.deleteInquiryInfo Start!", this.getClass().getName());
        log.info("삭제할 문의글 시퀀스: {}", pDTO.getNoticeSeq());

        log.info("연관된 댓글 삭제를 시작합니다.");
        commentMapper.deleteCommentsByNoticeSeq(pDTO.getNoticeSeq());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document query = new Document("noticeSeq", pDTO.getNoticeSeq());
        col.deleteOne(query);

        log.info("문의글(시퀀스: {}) 및 관련 댓글 삭제를 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.deleteInquiryInfo End!", this.getClass().getName());
    }

    // getInquiryInfo(NoticeDTO) 와 updateInquiryReadCnt(NoticeDTO) 는 서비스 단에서 통합되었으므로,
    // 아래 메서드들은 IInquiryMapper 인터페이스에서도 제거하거나 주석 처리하는 것을 권장합니다.

    @Override
    public NoticeDTO getInquiryInfo(NoticeDTO pDTO) throws Exception {
        return getInquiryInfo(pDTO, false); // 조회수 증가 없이 호출
    }

    @Override
    public void updateInquiryReadCnt(NoticeDTO pDTO) throws Exception {
        // 이 메서드는 getInquiryInfo(pDTO, true)로 대체되었습니다.
        // 직접 호출되지 않도록 비워두거나 삭제하는 것이 좋습니다.
    }

    @Override
    public void deleteInquiryByUserId(UserDTO pDTO) throws MongoException {
        log.info("{}.deleteInquiryByUserId Start! (Note: Performing UPDATE, not DELETE)", this.getClass().getName());

        String originalUserId = pDTO.getUserId();
        String withdrawnUserId = originalUserId + " (회원탈퇴)";

        log.info("문의글/댓글의 작성자 ID를 '{}'에서 '{}'(으)로 변경(업데이트)합니다.", originalUserId, withdrawnUserId);

        commentMapper.updateCommentUserIdForWithdrawal(originalUserId, withdrawnUserId);

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document filter = new Document("userId", originalUserId);
        Document updateDoc = new Document("$set", new Document("userId", withdrawnUserId));

        UpdateResult result = col.updateMany(filter, updateDoc);

        log.info("사용자 '{}'의 문의글 {}개가 '삭제' 대신 (회원탈퇴) 상태로 업데이트되었습니다.", originalUserId, result.getModifiedCount());
        log.info("{}.deleteInquiryByUserId End!", this.getClass().getName());
    }
}

package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import kopo.poly.dto.NoticeDTO;
import kopo.poly.dto.UserDTO;
import kopo.poly.persistance.mongodb.ICommentMapper;
import kopo.poly.persistance.mongodb.IInquiryMapper;
import kopo.poly.persistance.mongodb.INoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class InquiryMapper implements IInquiryMapper {

    private final MongoTemplate mongodb;
    private final ICommentMapper commentMapper; // 다른 맵퍼(댓글)와 연동

    private static final String colNM = "INQUIRY";

    /**
     * MongoDB의 counters 컬렉션을 사용하여 지정된 시퀀스 이름의 값을 1 증가시키고 반환합니다.
     * (DB의 AUTO_INCREMENT와 유사한 기능 구현)
     *
     * @param sequenceName 시퀀스로 사용할 이름 (e.g., "noticeseq")
     * @return 증가된 시퀀스 값 (문자열)
     */
    private String getNextSequence(String sequenceName) {
        MongoCollection<Document> counterCol = mongodb.getCollection("counters2");   // 자동증분 기능

        Document filter = new Document("_id", sequenceName);
        Document update = new Document("$inc", new Document("seq", 1));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER) // 업데이트 후의 문서를 반환
                .upsert(true); // 시퀀스가 없으면 새로 생성

        Document updatedDoc = counterCol.findOneAndUpdate(filter, update, options);
        return String.valueOf(updatedDoc.get("seq"));
    }

    /**
     * 문의글 목록을 조회합니다. 관리자는 전체 목록, 일반 사용자는 자신의 글만 조회합니다.
     *
     * @param pDTO userId가 포함된 DTO. null이거나 userId가 없으면 전체 조회
     * @return 조회된 문의글 DTO 리스트
     * @throws MongoException MongoDB 처리 중 오류 발생 시
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

        // [핵심 수정] 정렬 기준을 noticeYn 없이, 오직 등록일(regDt) 내림차순(최신순)으로 변경
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
     *
     * @param pDTO 삽입할 문의글 정보를 담은 DTO
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void insertInquiryInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.insertInquiryInfo Start!", this.getClass().getName());
        log.info("작성자: {}, 제목: {}", pDTO.getUserId(), pDTO.getTitle());

        MongoCollection<Document> col = mongodb.getCollection(colNM);

        // noticeSeq: 자동 증가 시퀀스로 설정
        String nextSeq = getNextSequence("noticeseq");
        pDTO.setNoticeSeq(nextSeq);
        log.info("생성된 문의글 시퀀스: {}", nextSeq);

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
                .append("chgDt", pDTO.getChgDt());
        col.insertOne(doc);

        log.info("문의글(시퀀스: {}) 삽입을 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.insertInquiryInfo End!", this.getClass().getName());
    }

    /**
     * 특정 문의글의 상세 정보를 조회합니다.
     *
     * @param pDTO 조회할 문의글의 noticeSeq가 포함된 DTO
     * @return 조회된 문의글 DTO, 없는 경우 null
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public NoticeDTO getInquiryInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.getInquiryInfo Start!", this.getClass().getName());
        log.info("조회할 문의글 시퀀스: {}", pDTO.getNoticeSeq());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document query = new Document("noticeSeq", pDTO.getNoticeSeq());
        Document doc = col.find(query).first();

        // Document를 DTO로 변환 (ObjectMapper 사용)
        NoticeDTO rDTO = (doc != null) ? new ObjectMapper().convertValue(doc, NoticeDTO.class) : null;

        if (rDTO == null) {
            log.warn("시퀀스 '{}'에 해당하는 문의글을 찾을 수 없습니다.", pDTO.getNoticeSeq());
        } else {
            log.info("시퀀스 '{}'에 해당하는 문의글을 성공적으로 조회했습니다.", pDTO.getNoticeSeq());
        }

        log.info("{}.getInquiryInfo End!", this.getClass().getName());
        return rDTO;
    }

    /**
     * 특정 문의글의 조회수를 1 증가시킵니다.
     *
     * @param pDTO 조회수를 증가시킬 문의글의 noticeSeq가 포함된 DTO
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void updateInquiryReadCnt(NoticeDTO pDTO) throws MongoException {
        log.info("{}.updateInquiryReadCnt Start!", this.getClass().getName());
        log.info("조회수를 증가시킬 문의글 시퀀스: {}", pDTO.getNoticeSeq());

        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document query = new Document("noticeSeq", pDTO.getNoticeSeq());
        Document update = new Document("$inc", new Document("readCnt", 1)); // readCnt 필드 1 증가
        col.updateOne(query, update);

        log.info("문의글(시퀀스: {}) 조회수 업데이트를 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.updateInquiryReadCnt End!", this.getClass().getName());
    }

    /**
     * 특정 문의글의 정보를 수정합니다.
     *
     * @param pDTO 수정할 정보가 담긴 DTO (noticeSeq, title, contents 등)
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void updateInquiryInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.updateInquiryInfo Start!", this.getClass().getName());
        log.info("수정할 문의글 시퀀스: {}", pDTO.getNoticeSeq());

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

        log.info("문의글(시퀀스: {}) 정보 수정을 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.updateInquiryInfo End!", this.getClass().getName());
    }

    /**
     * 특정 문의글과 그에 딸린 모든 댓글을 삭제합니다.
     *
     * @param pDTO 삭제할 문의글의 noticeSeq가 포함된 DTO
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void deleteInquiryInfo(NoticeDTO pDTO) throws MongoException {
        log.info("{}.deleteInquiryInfo Start!", this.getClass().getName());
        log.info("삭제할 문의글 시퀀스: {}", pDTO.getNoticeSeq());

        // 1. 연관된 댓글을 먼저 모두 삭제 (CommentMapper 호출)
        log.info("연관된 댓글 삭제를 시작합니다.");
        commentMapper.deleteCommentsByNoticeSeq(pDTO.getNoticeSeq());

        // 2. 문의글 본문 삭제
        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document query = new Document("noticeSeq", pDTO.getNoticeSeq());
        col.deleteOne(query);

        log.info("문의글(시퀀스: {}) 및 관련 댓글 삭제를 완료했습니다.", pDTO.getNoticeSeq());
        log.info("{}.deleteInquiryInfo End!", this.getClass().getName());
    }

    /**
     * 특정 사용자가 작성한 모든 문의글과 관련 댓글을 삭제합니다. (회원 탈퇴 시 사용)
     *
     * @param pDTO 삭제할 사용자의 userId가 포함된 DTO
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public void deleteInquiryByUserId(UserDTO pDTO) throws MongoException {
        // [주의] 이 메서드는 실제로 데이터를 삭제하지 않고, userId를 '회원탈퇴' 상태로 업데이트합니다.
        log.info("{}.deleteInquiryByUserId Start! (Note: Performing UPDATE, not DELETE)", this.getClass().getName());

        String originalUserId = pDTO.getUserId();
        String withdrawnUserId = originalUserId + " (회원탈퇴)";

        log.info("문의글/댓글의 작성자 ID를 '{}'에서 '{}'(으)로 변경(업데이트)합니다.", originalUserId, withdrawnUserId);

        // 1. 이 사용자가 작성한 모든 댓글의 userId를 업데이트합니다.
        //    CommentMapper의 관련 메서드를 호출해야 합니다.
        commentMapper.updateCommentUserIdForWithdrawal(originalUserId, withdrawnUserId);

        // 2. 이 사용자가 작성한 모든 문의글의 userId를 업데이트합니다.
        MongoCollection<Document> col = mongodb.getCollection(colNM);
        Document filter = new Document("userId", originalUserId);
        Document updateDoc = new Document("$set", new Document("userId", withdrawnUserId));

        // DeleteResult가 아닌 UpdateResult를 사용해야 합니다.
        UpdateResult result = col.updateMany(filter, updateDoc);

        log.info("사용자 '{}'의 문의글 {}개가 '삭제' 대신 (회원탈퇴) 상태로 업데이트되었습니다.", originalUserId, result.getModifiedCount());

        log.info("{}.deleteInquiryByUserId End!", this.getClass().getName());
    }
}

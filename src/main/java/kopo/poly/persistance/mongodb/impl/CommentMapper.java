package kopo.poly.persistance.mongodb.impl;

import com.mongodb.MongoException;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import kopo.poly.dto.CommentDTO;
import kopo.poly.persistance.mongodb.ICommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.eq;
import static kotlin.reflect.jvm.internal.impl.builtins.StandardNames.FqNames.collection;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommentMapper implements ICommentMapper {

    private final MongoTemplate mongodb;

    private static final String collectionName = "COMMENT";

    /**
     * 특정 문의글(Notice)에 달린 모든 댓글 목록을 조회합니다.
     *
     * @param noticeSeq 댓글을 조회할 문의글의 고유 시퀀스
     * @return 조회된 댓글 DTO 리스트
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public List<CommentDTO> selectCommentsByNoticeSeq(String noticeSeq) throws MongoException {
        log.info("{}.selectCommentsByNoticeSeq Start!", this.getClass().getName());
        log.info("조회할 문의글(Notice) 시퀀스: {}", noticeSeq);

        List<CommentDTO> rList = new ArrayList<>();
        MongoCollection<Document> col = mongodb.getCollection(collectionName);

        // noticeSeq가 일치하는 모든 문서를 찾아 커서로 반복 처리
        try (MongoCursor<Document> cursor = col.find(eq("noticeSeq", noticeSeq)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                CommentDTO dto = CommentDTO.builder()
                        .commentId(doc.getObjectId("_id").toHexString())
                        .noticeSeq(doc.getString("noticeSeq"))
                        .userId(doc.getString("userId"))
                        .comment(doc.getString("comment"))
                        .regDt(doc.getString("regDt"))
                        .build();
                rList.add(dto);
            }
        }
        log.info("문의글 시퀀스 '{}'에 대해 총 {}개의 댓글을 조회했습니다.", noticeSeq, rList.size());
        log.info("{}.selectCommentsByNoticeSeq End!", this.getClass().getName());
        return rList;
    }

    /**
     * 새로운 댓글을 DB에 삽입합니다.
     *
     * @param dto 삽입할 댓글 정보를 담은 DTO (noticeSeq, userId, comment, regDt 필요)
     * @return 삽입 성공 시 1
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public int insertComment(CommentDTO dto) throws MongoException {
        log.info("{}.insertComment Start!", this.getClass().getName());
        log.info("삽입할 댓글 정보 -> 문의글Seq: {}, 작성자: {}", dto.getNoticeSeq(), dto.getUserId());

        MongoCollection<Document> col = mongodb.getCollection(collectionName);
        Document doc = new Document()
                .append("noticeSeq", dto.getNoticeSeq())
                .append("userId", dto.getUserId())
                .append("comment", dto.getComment())
                .append("regDt", dto.getRegDt());

        col.insertOne(doc);
        log.info("댓글 삽입을 완료했습니다.");

        log.info("{}.insertComment End!", this.getClass().getName());
        return 1;
    }

    /**
     * 댓글 ID를 사용하여 특정 댓글 하나를 삭제합니다.
     *
     * @param commentId 삭제할 댓글의 고유 ID (_id)
     * @return 삭제된 댓글의 개수 (성공 시 1, 실패 시 0)
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public int deleteComment(String commentId) throws MongoException {
        log.info("{}.deleteComment Start!", this.getClass().getName());
        log.info("삭제할 댓글 ID: {}", commentId);

        MongoCollection<Document> col = mongodb.getCollection(collectionName);
        long deleted = col.deleteOne(eq("_id", new ObjectId(commentId))).getDeletedCount();

        log.info("댓글 삭제 결과: {}건 삭제됨", deleted);
        log.info("{}.deleteComment End!", this.getClass().getName());
        return (int) deleted;
    }

    /**
     * 댓글 ID를 사용하여 특정 댓글의 상세 정보를 조회합니다.
     *
     * @param commentId 조회할 댓글의 고유 ID (_id)
     * @return 조회된 댓글 DTO, 없는 경우 null
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public CommentDTO selectCommentById(String commentId) throws MongoException {
        log.info("{}.selectCommentById Start!", this.getClass().getName());
        log.info("조회할 댓글 ID: {}", commentId);

        MongoCollection<Document> col = mongodb.getCollection(collectionName);
        Document doc = col.find(eq("_id", new ObjectId(commentId))).first();

        if (doc == null) {
            log.warn("ID '{}'에 해당하는 댓글을 찾을 수 없습니다.", commentId);
            log.info("{}.selectCommentById End!", this.getClass().getName());
            return null;
        }

        log.info("ID '{}'에 해당하는 댓글을 성공적으로 조회했습니다.", commentId);
        log.info("{}.selectCommentById End!", this.getClass().getName());
        return CommentDTO.builder()
                .commentId(doc.getObjectId("_id").toHexString())
                .noticeSeq(doc.getString("noticeSeq"))
                .userId(doc.getString("userId"))
                .comment(doc.getString("comment"))
                .regDt(doc.getString("regDt"))
                .build();
    }

    /**
     * 특정 댓글의 내용을 수정합니다.
     *
     * @param dto 수정할 댓글 정보를 담은 DTO (commentId, comment 필요)
     * @return 수정 성공 시 true, 실패 시 false
     * @throws MongoException MongoDB 처리 중 오류 발생 시
     */
    @Override
    public boolean updateComment(CommentDTO dto) throws MongoException {
        log.info("{}.updateComment Start!", this.getClass().getName());
        log.info("수정할 댓글 ID: {}", dto.getCommentId());

        MongoCollection<Document> col = mongodb.getCollection(collectionName);
        UpdateResult result = col.updateOne(
                eq("_id", new ObjectId(dto.getCommentId())),
                Updates.set("comment", dto.getComment())
        );

        log.info("댓글 수정 결과: {}건 수정됨", result.getModifiedCount());
        log.info("{}.updateComment End!", this.getClass().getName());
        return result.getModifiedCount() > 0;
    }

    /**
     * 특정 문의글에 연결된 모든 댓글을 삭제합니다.
     * (문의글 삭제 또는 관련 로직에서 사용)
     *
     * @param noticeSeq 관련 댓글을 모두 삭제할 문의글의 고유 시퀀스
     */
    @Override
    public void deleteCommentsByNoticeSeq(String noticeSeq) {
        log.info("{}.deleteCommentsByNoticeSeq Start!", this.getClass().getName());
        log.info("연관된 모든 댓글을 삭제할 문의글 시퀀스: {}", noticeSeq);

        MongoCollection<Document> col = mongodb.getCollection(collectionName);

        // noticeSeq가 일치하는 모든 문서를 찾아서 삭제
        DeleteResult result = col.deleteMany(eq("noticeSeq", noticeSeq));
        log.info("'{}'번 문의글의 댓글 {}개가 삭제되었습니다.", noticeSeq, result.getDeletedCount());

        log.info("{}.deleteCommentsByNoticeSeq End!", this.getClass().getName());
    }

    @Override
    public void updateCommentUserIdForWithdrawal(String originalUserId, String withdrawnUserId) {
        log.info("{}.updateCommentUserIdForWithdrawal Start!", this.getClass().getName());
        log.info("댓글 작성자 ID를 '{}'에서 '{}'(으)로 변경합니다.", originalUserId, withdrawnUserId);

        // 1. 댓글 컬렉션을 가져옵니다.
        MongoCollection<Document> col = mongodb.getCollection(collectionName);

        // 2. 업데이트할 댓글을 필터링합니다. (작성자 ID가 일치하는 모든 댓글)
        Bson filter = eq("userId", originalUserId);

        // 3. userId 필드를 새로운 값으로 설정($set)하는 업데이트 문서를 만듭니다.
        Document updateDoc = new Document("$set", new Document("userId", withdrawnUserId));

        // 4. updateMany를 사용하여 일치하는 모든 댓글을 한 번에 업데이트합니다.
        UpdateResult result = col.updateMany(filter, updateDoc);
        log.info("사용자 '{}'의 댓글 {}개가 (회원탈퇴) 상태로 업데이트되었습니다.", originalUserId, result.getModifiedCount());

        log.info("{}.updateCommentUserIdForWithdrawal End!", this.getClass().getName());
    }
}
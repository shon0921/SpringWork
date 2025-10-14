package kopo.poly.persistance.mongodb;

import kopo.poly.dto.CommentDTO;

import java.util.List;

public interface ICommentMapper {

    // 댓글 목록 조회
    List<CommentDTO> selectCommentsByNoticeSeq(String noticeSeq);

    // 댓글 등록
    int insertComment(CommentDTO dto);

    // 댓글 삭제
    int deleteComment(String commentId);

    // 댓글 상세 조회
    CommentDTO selectCommentById(String commentId);

    boolean updateComment(CommentDTO dto);

    // 원글 삭제시 모든 댓글 삭제
    void deleteCommentsByNoticeSeq(String noticeSeq);

    void updateCommentUserIdForWithdrawal(String originalUserId, String withdrawnUserId);
}
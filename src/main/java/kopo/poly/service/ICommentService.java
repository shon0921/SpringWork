package kopo.poly.service;

import kopo.poly.dto.CommentDTO;

import java.util.List;

public interface ICommentService {

    // 특정 문의글(nSeq)의 댓글 목록 조회
    List<CommentDTO> getCommentsByNoticeSeq(String noticeSeq);

    // 댓글 등록
    boolean insertComment(CommentDTO commentDTO);

    // 댓글 삭제
    boolean deleteComment(String commentId);

    // 댓글 상세 조회 (작성자 확인용 등)
    CommentDTO getCommentById(String commentId);

    boolean updateComment(CommentDTO dto);
}
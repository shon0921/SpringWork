package kopo.poly.service.impl;

import kopo.poly.dto.CommentDTO;


import kopo.poly.persistance.mongodb.ICommentMapper;
import kopo.poly.service.ICommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentService implements ICommentService {

    private final ICommentMapper commentMapper;

    /**
     * 특정 문의글에 달린 모든 댓글 목록을 조회합니다.
     *
     * @param noticeSeq 댓글을 조회할 문의글의 고유 시퀀스
     * @return 조회된 댓글 DTO 리스트
     */
    @Override
    public List<CommentDTO> getCommentsByNoticeSeq(String noticeSeq) {
        log.info("{}.getCommentsByNoticeSeq Start!", this.getClass().getName());
        log.info("조회할 문의글(Notice) 시퀀스: {}", noticeSeq);

        List<CommentDTO> commentList = commentMapper.selectCommentsByNoticeSeq(noticeSeq);
        log.info("총 {}개의 댓글을 조회했습니다.", commentList.size());

        log.info("{}.getCommentsByNoticeSeq End!", this.getClass().getName());
        return commentList;
    }

    /**
     * 새로운 댓글을 등록합니다.
     *
     * @param commentDTO 등록할 댓글 정보를 담은 DTO
     * @return 등록 성공 시 true, 실패 시 false
     */
    @Override
    public boolean insertComment(CommentDTO commentDTO) {
        log.info("{}.insertComment Start!", this.getClass().getName());
        log.info("등록할 댓글 정보 -> 문의글Seq: {}, 작성자: {}", commentDTO.getNoticeSeq(), commentDTO.getUserId());

        boolean res = commentMapper.insertComment(commentDTO) > 0;
        log.info("댓글 등록 결과: {}", res);

        log.info("{}.insertComment End!", this.getClass().getName());
        return res;
    }

    /**
     * 특정 댓글 하나를 삭제합니다.
     *
     * @param commentId 삭제할 댓글의 고유 ID (_id)
     * @return 삭제 성공 시 true, 실패 시 false
     */
    @Override
    public boolean deleteComment(String commentId) {
        log.info("{}.deleteComment Start!", this.getClass().getName());
        log.info("삭제할 댓글 ID: {}", commentId);

        boolean res = commentMapper.deleteComment(commentId) > 0;
        log.info("댓글 삭제 결과: {}", res);

        log.info("{}.deleteComment End!", this.getClass().getName());
        return res;
    }

    /**
     * 특정 댓글 하나의 상세 정보를 조회합니다.
     *
     * @param commentId 조회할 댓글의 고유 ID (_id)
     * @return 조회된 댓글 DTO, 없는 경우 null
     */
    @Override
    public CommentDTO getCommentById(String commentId) {
        log.info("{}.getCommentById Start!", this.getClass().getName());
        log.info("조회할 댓글 ID: {}", commentId);

        CommentDTO comment = commentMapper.selectCommentById(commentId);
        if (comment == null) {
            log.warn("ID '{}'에 해당하는 댓글을 찾지 못했습니다.", commentId);
        } else {
            log.info("댓글 조회를 완료했습니다.");
        }

        log.info("{}.getCommentById End!", this.getClass().getName());
        return comment;
    }

    /**
     * 특정 댓글의 내용을 수정합니다.
     *
     * @param dto 수정할 댓글 정보를 담은 DTO (commentId, comment 필요)
     * @return 수정 성공 시 true, 실패 시 false
     */
    @Override
    public boolean updateComment(CommentDTO dto) {
        log.info("{}.updateComment Start!", this.getClass().getName());
        log.info("수정할 댓글 ID: {}", dto.getCommentId());

        boolean res = commentMapper.updateComment(dto);
        log.info("댓글 수정 결과: {}", res);

        log.info("{}.updateComment End!", this.getClass().getName());
        return res;
    }
}
package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.CommentDTO;
import kopo.poly.service.impl.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping(value = "/comment")
@RequiredArgsConstructor
@Controller
public class CommentController {

    private final CommentService commentService;

    // 댓글 목록 조회

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> getComments(@RequestParam("nSeq") String nSeq) {

        log.info("{}.listComment Start!", this.getClass().getName());
        Map<String, Object> result = new HashMap<>();
        List<CommentDTO> commentList = commentService.getCommentsByNoticeSeq(nSeq);

        result.put("message", "SUCCESS");
        result.put("data", commentList);
        log.info("{}.listComment End!", this.getClass().getName());
        return result;
    }

    // 댓글 등록
    @PostMapping("/add")
    @ResponseBody
    public Map<String, Object> addComment(@RequestBody CommentDTO commentDTO, HttpSession session) {

        log.info("{}.addComment Start!", this.getClass().getName());

        Map<String, Object> result = new HashMap<>();
        String userId = (String) session.getAttribute("userId");

        if (userId == null) {
            result.put("message", "LOGIN_REQUIRED");
            return result;
        }

        log.info("userId: {}", userId);
        log.info("comment: {}", commentDTO.getComment());

        commentDTO.setUserId(userId);
        commentDTO.setRegDt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("dt: {}", commentDTO.getRegDt());
        boolean success = commentService.insertComment(commentDTO);

        result.put("message", success ? "SUCCESS" : "FAIL");

        log.info("{}.addComment End!", this.getClass().getName());

        return result;
    }

    // 댓글 삭제
    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> deleteComment(@RequestParam("commentId") String commentId,
                                             HttpSession session) {

        log.info("{}.deleteComment Start!", this.getClass().getName());

        Map<String, Object> result = new HashMap<>();
        String sessionUserId = (String) session.getAttribute("userId");
        String adminYn = (String) session.getAttribute("adminYn");  // 관리자 여부 추가

        CommentDTO comment = commentService.getCommentById(commentId);

        if (comment == null) {
            result.put("message", "NOT_FOUND");
            return result;
        }

        if (!comment.getUserId().equals(sessionUserId) && !"Y".equals(adminYn)) {
            result.put("message", "UNAUTHORIZED");
            return result;
        }

        boolean deleted = commentService.deleteComment(commentId);

        result.put("message", deleted ? "SUCCESS" : "FAIL");
        log.info("{}.deleteComment End!", this.getClass().getName());
        return result;


    }

    // 댓글 수정
    @PostMapping("/update")
    @ResponseBody
    public Map<String, Object> updateComment(@RequestBody CommentDTO dto, HttpSession session) {
        log.info("{}.updateComment Start!", this.getClass().getName());

        Map<String, Object> result = new HashMap<>();

        String sessionUserId = (String) session.getAttribute("userId");
        String sessionAdminYn = (String) session.getAttribute("adminYn");

        // 기존 댓글 조회
        CommentDTO originalComment = commentService.getCommentById(dto.getCommentId());
        if (originalComment == null) {
            result.put("message", "NOT_FOUND");
            return result;
        }

        // 권한 확인: 작성자 또는 관리자만 수정 가능
        if (!originalComment.getUserId().equals(sessionUserId) && !"Y".equals(sessionAdminYn)) {
            result.put("message", "UNAUTHORIZED");
            return result;
        }

        // 댓글 내용 수정
        // 댓글 내용 수정
        originalComment.setComment(dto.getComment());
        boolean updated = commentService.updateComment(originalComment);

        result.put("message", updated ? "SUCCESS" : "FAIL");
        log.info("{}.updateComment End!", this.getClass().getName());
        return result;
    }
}
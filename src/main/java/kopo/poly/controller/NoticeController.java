package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.MsgDTO;
import kopo.poly.dto.NoticeDTO;
import kopo.poly.service.INoticeService;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


/*
 * Controller 선언해야만 Spring 프레임워크에서 Controller인지 인식 가능
 * 자바 서블릿 역할 수행
 *
 * slf4j는 스프링 프레임워크에서 로그 처리하는 인터페이스 기술이며,
 * 로그처리 기술인 log4j와 logback과 인터페이스 역할 수행함
 * 스프링 프레임워크는 기본으로 logback을 채택해서 로그 처리함
 * */
@Slf4j
@RequestMapping(value = "/notice")
@RequiredArgsConstructor
@Controller
public class NoticeController {

    private final INoticeService noticeService;

    /**
     * 공지사항 리스트 조회
     */
    @GetMapping(value = "noticeList")
    @ResponseBody
    public ResponseEntity<?> noticeList(HttpSession session) throws Exception {
        log.info("{}.noticeList Start!", this.getClass().getName());

        List<NoticeDTO> rList = Optional.ofNullable(noticeService.getNoticeList())
                .orElseGet(ArrayList::new);

        String adminYn = CmmUtil.nvl((String) session.getAttribute("adminYn"));

        Map<String, Object> resData = new HashMap<>();
        resData.put("noticeList", rList);
        resData.put("adminYn", adminYn);


        log.info("{}.noticeList End!", this.getClass().getName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", resData));
    }

    /**
     * 공지사항 글 등록
     */
    @ResponseBody
    @PostMapping(value = "noticeInsert")
    public ResponseEntity<?> noticeInsert(@RequestBody NoticeDTO pDTO, HttpSession session)  {
        log.info("{}.noticeInsert Start!", this.getClass().getName());

        String msg;
        MsgDTO dto;

        try {
            String adminYn = CmmUtil.nvl((String) session.getAttribute("adminYn"));

            if (!"Y".equals(adminYn)) {
                msg = "관리자만 등록할 수 있습니다.";
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(CommonResponse.of(HttpStatus.FORBIDDEN, msg, null));
            }


            String userId = CmmUtil.nvl((String) session.getAttribute("userId"));
            if (userId.isEmpty()) {
                msg = "로그인이 필요합니다.";
                dto = new MsgDTO(0, msg, null, null,null,null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.of(HttpStatus.UNAUTHORIZED, msg, dto));
            }

            // 유효성 검사
            if (CmmUtil.nvl(pDTO.getTitle()).isEmpty() || CmmUtil.nvl(pDTO.getContents()).isEmpty()) {
                throw new IllegalArgumentException("제목과 내용을 입력해야 합니다.");
            }

            log.info("userId: {}, title: {}, noticeYn: {}, contents: {}", userId, pDTO.getTitle(), pDTO.getNoticeYn(), pDTO.getContents());

            // 사용자 정보 및 날짜 설정
            pDTO.setUserId(userId);
            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            pDTO.setRegDt(currentDateTime);
            pDTO.setChgDt(currentDateTime);

            // DB 저장
            noticeService.insertNoticeInfo(pDTO);

            msg = "등록되었습니다.";
            dto = new MsgDTO(1, msg, null, null,null,null);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CommonResponse.of(HttpStatus.CREATED, msg, dto));

        } catch (Exception e) {
            msg = "실패하였습니다: " + e.getMessage();
            log.error("등록 중 오류", e);
            dto = new MsgDTO(0, msg, null, null,null,null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, msg, dto));
        } finally {
            log.info("{}.noticeInsert End!", this.getClass().getName());
        }
    }


    /**
     * 공지사항 상세보기
     */
    @GetMapping(value = "noticeInfo")
    @ResponseBody
    public ResponseEntity<?> noticeInfo(@RequestParam("nSeq") String nSeq,
                                        HttpSession session) throws Exception {

        log.info("{}.noticeInfo Start!", this.getClass().getName());

        NoticeDTO pDTO = new NoticeDTO();
        pDTO.setNoticeSeq(nSeq);

        NoticeDTO rDTO = Optional.ofNullable(noticeService.getNoticeInfo(pDTO, true))
                .orElseGet(NoticeDTO::new);

        String sessionUserId = CmmUtil.nvl((String) session.getAttribute("userId"));
        String adminYn = CmmUtil.nvl((String) session.getAttribute("adminYn")); // 세션에서 adminYn 가져오기

        Map<String, Object> resData = new HashMap<>();
        resData.put("notice", rDTO);
        resData.put("sessionUserId", sessionUserId);
        resData.put("adminYn", adminYn); // adminYn 추가

        log.info("{}.noticeInfo End!", this.getClass().getName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", resData));
    }


    /**
     * 공지사항 수정 화면 이동
     */
    @GetMapping("/noticeEditInfo")
    public String noticeEditInfo(@RequestParam("nSeq") String nSeq, Model model) throws Exception {

        log.info("{}.noticeEditInfo Start!", this.getClass().getName());

        NoticeDTO pDTO = new NoticeDTO();
        pDTO.setNoticeSeq(nSeq);

        NoticeDTO rDTO = Optional.ofNullable(noticeService.getNoticeInfo(pDTO, false))
                .orElseGet(NoticeDTO::new);

        model.addAttribute("rDTO", rDTO);

        log.info("{} seqNumber: {}", this.getClass().getName(), rDTO.getNoticeSeq());
        log.info("{}.noticeEditInfo End!", this.getClass().getName());

        return "notice/noticeEditInfo";
    }

    /**
     * 공지사항 글 수정
     */
    @ResponseBody
    @PostMapping(value = "noticeUpdate")
    public ResponseEntity<?> noticeUpdate(HttpSession session, HttpServletRequest request) {

        log.info("{}.noticeUpdate Start!", this.getClass().getName());

        String msg = "";
        MsgDTO dto;

        try {

            String adminYn = CmmUtil.nvl((String) session.getAttribute("adminYn"));

            if (!"Y".equals(adminYn)) {
                msg = "관리자만 수정할 수 있습니다.";
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(CommonResponse.of(HttpStatus.FORBIDDEN, msg, null));
            }

            String userId = CmmUtil.nvl((String) session.getAttribute("userId")); // 세션 키 'userId'로 통일
            String nSeq = CmmUtil.nvl(request.getParameter("nSeq"));
            String title = CmmUtil.nvl(request.getParameter("title"));
            String noticeYn = CmmUtil.nvl(request.getParameter("noticeYn"));
            String contents = CmmUtil.nvl(request.getParameter("contents"));

            log.info("userId : {} / nSeq : {} / title : {} / noticeYn : {} / contents : {} ",
                    userId, nSeq, title, noticeYn, contents);

            NoticeDTO pDTO = new NoticeDTO();
            pDTO.setUserId(userId);
            pDTO.setNoticeSeq(nSeq);
            pDTO.setTitle(title);
            pDTO.setNoticeYn(noticeYn);
            pDTO.setContents(contents);

            noticeService.updateNoticeInfo(pDTO);

            msg = "수정되었습니다.";

        } catch (Exception e) {
            msg = "실패하였습니다. : " + e.getMessage();
            log.info(e.toString());

        } finally {
            dto = new MsgDTO(1, msg, null, null,null,null);

            log.info("{}.noticeUpdate", msg);
            log.info("{}.noticeUpdate End!", this.getClass().getName());
        }

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, msg, dto));
    }

    /**
     * 공지사항 글 삭제
     */
    @ResponseBody
    @PostMapping(value = "noticeDelete")
    public ResponseEntity<?> noticeDelete(HttpServletRequest request, HttpSession session) {

        log.info("{}.noticeDelete Start!", this.getClass().getName());

        String msg = "";
        MsgDTO dto;

        try {

            String adminYn = CmmUtil.nvl((String) session.getAttribute("adminYn"));

            if (!"Y".equals(adminYn)) {
                msg = "관리자만 삭제할 수 있습니다.";
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(CommonResponse.of(HttpStatus.FORBIDDEN, msg, null));
            }

            String nSeq = CmmUtil.nvl(request.getParameter("nSeq"));
            log.info("nSeq : {}", nSeq);

            NoticeDTO pDTO = new NoticeDTO();
            pDTO.setNoticeSeq(nSeq);

            noticeService.deleteNoticeInfo(pDTO);

            msg = "삭제되었습니다.";

        } catch (Exception e) {
            msg = "실패하였습니다. : " + e.getMessage();
            log.info(e.toString());

        } finally {
            dto = new MsgDTO(1, msg, null, null,null,null);
            log.info("{}.noticeDelete End!", this.getClass().getName());
        }

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, msg, dto));
    }

    @GetMapping(value = "GetNoticeInfo")
    @ResponseBody
    public ResponseEntity<?> getNoticeInfo(@RequestParam("nSeq") String nSeq) throws Exception {
        NoticeDTO pDTO = new NoticeDTO();
        pDTO.setNoticeSeq(nSeq);

        NoticeDTO rDTO = Optional.ofNullable(noticeService.getNoticeInfo(pDTO, false))
                .orElseGet(NoticeDTO::new);

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", rDTO));
    }

    /**
     * 공지사항 좋아요 추가
     */
    @PostMapping("/noticeLike")
    @ResponseBody
    public ResponseEntity<?> noticeLike(@RequestParam("nSeq") String nSeq, HttpSession session) throws Exception {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");

        NoticeDTO pDTO = new NoticeDTO();
        pDTO.setNoticeSeq(nSeq);
        pDTO.setUserId(userId);

        noticeService.addLike(pDTO);

        // 업데이트된 좋아요 수와 현재 사용자의 좋아요 상태 반환
        Map<String, Object> resData = new HashMap<>();
        resData.put("count", noticeService.getLikeCount(nSeq));
        resData.put("liked", true);

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", resData));
    }

    @PostMapping("/noticeCancelLike")
    @ResponseBody
    public ResponseEntity<?> noticeCancelLike(@RequestParam("nSeq") String nSeq, HttpSession session) throws Exception {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");

        NoticeDTO pDTO = new NoticeDTO();
        pDTO.setNoticeSeq(nSeq);
        pDTO.setUserId(userId);

        noticeService.cancelLike(pDTO);

        Map<String, Object> resData = new HashMap<>();
        resData.put("count", noticeService.getLikeCount(nSeq));
        resData.put("liked", false);

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", resData));
    }
    /**
     * 공지사항 좋아요 상태 및 카운트 조회
     */
    @GetMapping("/likeCount")
    @ResponseBody
    public ResponseEntity<?> getLikeCount(@RequestParam("nSeq") String nSeq, HttpSession session) {
        log.info("{}.getLikeCount Start!", this.getClass().getName());

        String userId = CmmUtil.nvl((String) session.getAttribute("userId"));

        Map<String, Object> resData = new HashMap<>();
        try {
            int count = noticeService.getLikeCount(nSeq); // 총 좋아요 수
            boolean liked = userId.isEmpty() ? false : noticeService.isLiked(nSeq, userId); // 로그인한 사용자의 좋아요 여부

            resData.put("count", count);
            resData.put("liked", liked);

            log.info("좋아요 상태: {}, 총 수: {}", liked, count);
            return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", resData));

        } catch (Exception e) {
            log.error("좋아요 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "FAIL", null));
        } finally {
            log.info("{}.getLikeCount End!", this.getClass().getName());
        }
    }

}

package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.MsgDTO;
import kopo.poly.dto.NoticeDTO;
import kopo.poly.dto.UserDTO;
import kopo.poly.service.IInquiryService;
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
@RequestMapping(value = "/inquiry")
@RequiredArgsConstructor
@Controller
public class InquiryController {

    private final IInquiryService inquiryService;

    /*
        문의하기 리스트

        관리자 계정이면 다보이고 아니면 본인 것만 보임
     */
    @GetMapping(value = "inquiryList")
    @ResponseBody
    public ResponseEntity<?> inquiryList(HttpSession session) throws Exception {
        log.info("{}.inquiryList Start!", this.getClass().getName());

        String userId = (String) session.getAttribute("userId");
        String adminYn = (String) session.getAttribute("adminYn");

        if (userId == null || adminYn == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.of(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null));
        }

        NoticeDTO pDTO = new NoticeDTO();
        pDTO.setUserId(userId);

        List<NoticeDTO> rList;

        if ("Y".equalsIgnoreCase(adminYn)) {
            rList = inquiryService.getInquiryList(null); // 관리자: 전체 조회
        } else {
            rList = inquiryService.getInquiryList(pDTO); // 사용자: 본인 글만
        }

        log.info("{}.inquiryList End!", this.getClass().getName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", rList));
    }

    /**
     * 공지사항 글 등록
     */
    @ResponseBody
    @PostMapping(value = "inquiryInsert")
    public ResponseEntity<?> inquiryInsert(@RequestBody NoticeDTO pDTO, HttpSession session)  {
        log.info("{}.inquiryInsert Start!", this.getClass().getName());

        String msg;
        MsgDTO dto;

        try {
            String userId = CmmUtil.nvl((String) session.getAttribute("userId"));
            if (userId.isEmpty()) {
                msg = "로그인이 필요합니다.";
                dto = MsgDTO.builder()
                        .result(0)
                        .msg(msg)
                        .build();
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
            inquiryService.insertInquiryInfo(pDTO);

            msg = "등록되었습니다.";
            dto = MsgDTO.builder()
                    .result(1)
                    .msg(msg)
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CommonResponse.of(HttpStatus.CREATED, msg, dto));

        } catch (Exception e) {
            msg = "실패하였습니다: " + e.getMessage();
            log.error("등록 중 오류", e);
            dto = MsgDTO.builder()
                    .result(0)
                    .msg(msg)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, msg, dto));
        } finally {
            log.info("{}.noticeInsert End!", this.getClass().getName());
        }
    }



    /**
     * 공지사항 상세보기
     */
    @GetMapping(value = "inquiryInfo")
    @ResponseBody
    public ResponseEntity<?> noticeInfo(@RequestParam("nSeq") String nSeq,
                                        HttpSession session) throws Exception {

        log.info("{}.inquiryInfo Start!", this.getClass().getName());

        NoticeDTO pDTO = new NoticeDTO();
        pDTO.setNoticeSeq(nSeq);

        NoticeDTO rDTO = Optional.ofNullable(inquiryService.getInquiryInfo(pDTO, true))
                .orElseGet(NoticeDTO::new);

        String sessionUserId = CmmUtil.nvl((String) session.getAttribute("userId"));

        Map<String, Object> resData = new HashMap<>();
        resData.put("notice", rDTO);
        resData.put("sessionUserId", sessionUserId);

        log.info("{}.noticeInfo End!", this.getClass().getName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", resData));
    }


    /**
     * 공지사항 수정 화면 이동
     */
    @GetMapping("/inquiryEditInfo")
    public String inquiryEditInfo(@RequestParam("nSeq") String nSeq, Model model) throws Exception {

        log.info("{}.inquiryEditInfo Start!", this.getClass().getName());

        NoticeDTO pDTO = new NoticeDTO();
        pDTO.setNoticeSeq(nSeq);

        NoticeDTO rDTO = Optional.ofNullable(inquiryService.getInquiryInfo(pDTO, false))
                .orElseGet(NoticeDTO::new);

        model.addAttribute("rDTO", rDTO);

        log.info("{}. seqNumber: {}", this.getClass().getName(), rDTO.getNoticeSeq());

        log.info("{}.inquiryEditInfo End!", this.getClass().getName());

        return "inquiry/inquiryEditInfo";
    }

    /**
     * 공지사항 글 수정
     */
    @ResponseBody
    @PostMapping(value = "inquiryUpdate")
    public ResponseEntity<?> inquiryUpdate(HttpSession session, HttpServletRequest request) {

        log.info("{}.inquiryUpdate Start!", this.getClass().getName());

        String msg = "";
        MsgDTO dto;

        try {
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

            inquiryService.updateInquiryInfo(pDTO);

            msg = "수정되었습니다.";

        } catch (Exception e) {
            msg = "실패하였습니다. : " + e.getMessage();
            log.info(e.toString());

        } finally {
            dto = new MsgDTO(1, msg, null, null,null,null);
            log.info("{}.inquiryUpdate End!", this.getClass().getName());
        }

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, msg, dto));
    }

    /**
     * 공지사항 글 삭제
     */
    @ResponseBody
    @PostMapping(value = "inquiryDelete")
    public ResponseEntity<?> inquiryDelete(HttpServletRequest request) {

        log.info("{}.inquiryDelete Start!", this.getClass().getName());

        String msg = "";
        MsgDTO dto;

        try {
            String nSeq = CmmUtil.nvl(request.getParameter("nSeq"));
            log.info("nSeq : {}", nSeq);

            NoticeDTO pDTO = new NoticeDTO();
            pDTO.setNoticeSeq(nSeq);

            inquiryService.deleteInquiryInfo(pDTO);

            msg = "삭제되었습니다.";

        } catch (Exception e) {
            msg = "실패하였습니다. : " + e.getMessage();
            log.info(e.toString());

        } finally {
            dto = new MsgDTO(1, msg, null, null,null,null);
            log.info("{}.inquiryDelete End!", this.getClass().getName());
        }

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, msg, dto));
    }

    @GetMapping(value = "GetInquiryInfo")
    @ResponseBody
    public ResponseEntity<?> getInquiryInfo(@RequestParam("nSeq") String nSeq) throws Exception {
        NoticeDTO pDTO = new NoticeDTO();
        pDTO.setNoticeSeq(nSeq);

        NoticeDTO rDTO = Optional.ofNullable(inquiryService.getInquiryInfo(pDTO, false))
                .orElseGet(NoticeDTO::new);

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "SUCCESS", rDTO));
    }

}

package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.MsgDTO;
import kopo.poly.dto.NoticeDTO;
import kopo.poly.service.IInquiryService;
import kopo.poly.service.impl.S3Uploader;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequestMapping(value = "/inquiry")
@RequiredArgsConstructor
@Controller
public class InquiryController {

    private final IInquiryService inquiryService;
    private final S3Uploader s3Uploader;

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

    @ResponseBody
    @PostMapping(value = "inquiryInsert")
    public ResponseEntity<?> inquiryInsert(@RequestPart("pDTO") NoticeDTO pDTO,
                                           @RequestPart(value = "file", required = false) MultipartFile file,
                                           HttpSession session) {
        log.info("{}.inquiryInsert Start!", this.getClass().getName());

        String msg;
        MsgDTO dto;

        try {
            String userId = CmmUtil.nvl((String) session.getAttribute("userId"));
            if (userId.isEmpty()) {
                msg = "로그인이 필요합니다.";
                dto = MsgDTO.builder().result(0).msg(msg).build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(CommonResponse.of(HttpStatus.UNAUTHORIZED, msg, dto));
            }

            if (CmmUtil.nvl(pDTO.getTitle()).isEmpty() || CmmUtil.nvl(pDTO.getContents()).isEmpty()) {
                throw new IllegalArgumentException("제목과 내용을 입력해야 합니다.");
            }

            if (file != null && !file.isEmpty()) {
                String imageUrl = s3Uploader.upload(file, "inquiry");
                pDTO.setImageUrl(imageUrl);
            }

            pDTO.setUserId(userId);
            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            pDTO.setRegDt(currentDateTime);
            pDTO.setChgDt(currentDateTime);

            inquiryService.insertInquiryInfo(pDTO);

            msg = "등록되었습니다.";
            dto = MsgDTO.builder().result(1).msg(msg).build();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CommonResponse.of(HttpStatus.CREATED, msg, dto));

        } catch (Exception e) {
            msg = "실패하였습니다: " + e.getMessage();
            log.error("등록 중 오류", e);
            dto = MsgDTO.builder().result(0).msg(msg).build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, msg, dto));
        } finally {
            log.info("{}.inquiryInsert End!", this.getClass().getName());
        }
    }


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

    @ResponseBody
    @PostMapping(value = "inquiryUpdate")
    public ResponseEntity<?> inquiryUpdate(@RequestParam("nSeq") String nSeq,
                                           @RequestParam("title") String title,
                                           @RequestParam("contents") String contents,
                                           @RequestParam(value = "file", required = false) MultipartFile file,
                                           HttpSession session) {

        log.info("{}.inquiryUpdate Start!", this.getClass().getName());

        String msg = "";
        MsgDTO dto;

        try {
            String userId = CmmUtil.nvl((String) session.getAttribute("userId"));

            NoticeDTO pDTO = new NoticeDTO();
            pDTO.setUserId(userId);
            pDTO.setNoticeSeq(nSeq);
            pDTO.setTitle(title);
            pDTO.setContents(contents);
            pDTO.setNoticeYn("N");

            if (file != null && !file.isEmpty()) {
                String imageUrl = s3Uploader.upload(file, "inquiry");
                pDTO.setImageUrl(imageUrl);
            }

            inquiryService.updateInquiryInfo(pDTO);

            msg = "수정되었습니다.";
            dto = new MsgDTO(1, msg, null, null, null, null);

        } catch (Exception e) {
            msg = "실패하였습니다. : " + e.getMessage();
            log.info(e.toString());
            dto = new MsgDTO(0, msg, null, null, null, null);
        } finally {
            log.info("{}.inquiryUpdate End!", this.getClass().getName());
        }

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, msg, dto));
    }

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
            dto = new MsgDTO(0, msg, null, null, null, null);
        } finally {
            dto = new MsgDTO(1, msg, null, null, null, null);
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

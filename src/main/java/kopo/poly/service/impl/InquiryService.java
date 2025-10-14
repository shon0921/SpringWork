package kopo.poly.service.impl;

import kopo.poly.dto.NoticeDTO;
import kopo.poly.persistance.mongodb.IInquiryMapper;
import kopo.poly.persistance.mongodb.INoticeMapper;
import kopo.poly.service.IInquiryService;
import kopo.poly.service.INoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class InquiryService implements IInquiryService {

    private final IInquiryMapper inquiryMapper;

    /**
     * 문의글 목록을 조회합니다.
     * 관리자는 전체, 일반 사용자는 자신의 글만 조회하도록 Mapper에 위임합니다.
     *
     * @param pDTO userId가 포함된 DTO. null이거나 userId가 없으면 전체 조회
     * @return 조회된 문의글 DTO 리스트
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public List<NoticeDTO> getInquiryList(NoticeDTO pDTO) throws Exception {
        log.info("{}.getInquiryList Start!", this.getClass().getName());

        if (pDTO != null && pDTO.getUserId() != null && !pDTO.getUserId().isEmpty()) {
            log.info("조회 요청 사용자 ID: {}", pDTO.getUserId());
        } else {
            log.info("전체 문의글 목록 조회 요청입니다.");
        }

        List<NoticeDTO> inquiryList = inquiryMapper.getInquiryList(pDTO);
        log.info("총 {}건의 문의글 목록 조회를 완료했습니다.", inquiryList.size());

        log.info("{}.getInquiryList End!", this.getClass().getName());
        return inquiryList;
    }

    /**
     * 특정 문의글의 상세 정보를 조회합니다.
     * 조회수 증가 옵션을 통해 글을 읽을 때 조회수를 올릴 수 있습니다.
     *
     * @param pDTO            조회할 문의글의 noticeSeq가 포함된 DTO
     * @param increaseReadCnt 조회수를 증가시킬지 여부 (true: 증가, false: 증가 안 함)
     * @return 조회된 문의글 DTO, 없는 경우 null
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public NoticeDTO getInquiryInfo(NoticeDTO pDTO, boolean increaseReadCnt) throws Exception {
        log.info("{}.getInquiryInfo Start!", this.getClass().getName());
        log.info("상세 조회할 문의글 시퀀스: {}", pDTO.getNoticeSeq());

        // 조회수 증가 옵션이 true일 경우, Mapper를 호출하여 조회수를 1 증가시킴
        if (increaseReadCnt) {
            log.info("문의글(시퀀스: {})의 조회수를 증가시킵니다.", pDTO.getNoticeSeq());
            inquiryMapper.updateInquiryReadCnt(pDTO);
        } else {
            log.info("조회수를 증가시키지 않습니다.");
        }

        // Mapper를 호출하여 문의글 상세 정보 조회
        NoticeDTO rDTO = inquiryMapper.getInquiryInfo(pDTO);
        if (rDTO != null) {
            log.info("문의글(시퀀스: {}) 상세 정보 조회를 완료했습니다.", pDTO.getNoticeSeq());
        } else {
            log.warn("문의글(시퀀스: {}) 정보를 찾을 수 없습니다.", pDTO.getNoticeSeq());
        }

        log.info("{}.getInquiryInfo End!", this.getClass().getName());
        return rDTO;
    }

    /**
     * 새로운 문의글을 등록합니다.
     *
     * @param pDTO 등록할 문의글 정보를 담은 DTO
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public void insertInquiryInfo(NoticeDTO pDTO) throws Exception {
        log.info("{}.insertInquiryInfo Start!", this.getClass().getName());
        log.info("새 문의글 등록 -> 작성자: {}, 제목: {}", pDTO.getUserId(), pDTO.getTitle());

        inquiryMapper.insertInquiryInfo(pDTO);

        log.info("새 문의글 등록을 완료했습니다.");
        log.info("{}.insertInquiryInfo End!", this.getClass().getName());
    }

    /**
     * 특정 문의글의 내용을 수정합니다.
     *
     * @param pDTO 수정할 문의글 정보가 담긴 DTO (noticeSeq 필요)
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public void updateInquiryInfo(NoticeDTO pDTO) throws Exception {
        log.info("{}.updateInquiryInfo Start!", this.getClass().getName());
        log.info("문의글 수정 -> 시퀀스: {}", pDTO.getNoticeSeq());

        inquiryMapper.updateInquiryInfo(pDTO);

        log.info("문의글 수정을 완료했습니다.");
        log.info("{}.updateInquiryInfo End!", this.getClass().getName());
    }

    /**
     * 특정 문의글과 관련 댓글을 모두 삭제합니다.
     *
     * @param pDTO 삭제할 문의글의 noticeSeq가 포함된 DTO
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public void deleteInquiryInfo(NoticeDTO pDTO) throws Exception {
        log.info("{}.deleteInquiryInfo Start!", this.getClass().getName());
        log.info("문의글 삭제 -> 시퀀스: {}", pDTO.getNoticeSeq());

        // Mapper는 내부적으로 댓글 삭제 후 문의글을 삭제함
        inquiryMapper.deleteInquiryInfo(pDTO);

        log.info("문의글 삭제를 완료했습니다.");
        log.info("{}.deleteInquiryInfo End!", this.getClass().getName());
    }
}
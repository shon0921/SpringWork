package kopo.poly.service.impl;

import kopo.poly.dto.NoticeDTO;
import kopo.poly.persistance.mongodb.INoticeMapper;
import kopo.poly.service.INoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class NoticeService implements INoticeService {

    private final INoticeMapper noticeMapper;

    /**
     * 모든 공지사항 목록을 조회합니다.
     *
     * @return 조회된 공지사항 DTO 리스트
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public List<NoticeDTO> getNoticeList() throws Exception {
        log.info("{}.getNoticeList Start!", this.getClass().getName());

        log.info("Mapper를 호출하여 전체 공지사항 목록 조회를 시작합니다.");
        List<NoticeDTO> noticeList = noticeMapper.getNoticeList();
        log.info("총 {}건의 공지사항 목록 조회를 완료했습니다.", noticeList.size());

        log.info("{}.getNoticeList End!", this.getClass().getName());
        return noticeList;
    }

    /**
     * 특정 공지사항의 상세 정보를 조회합니다.
     * 조회수 증가 옵션을 통해 글을 읽을 때 조회수를 올릴 수 있습니다.
     *
     * @param pDTO 조회할 공지사항의 noticeSeq가 포함된 DTO
     * @param type 조회수를 증가시킬지 여부 (true: 증가, false: 증가 안 함)
     * @return 조회된 공지사항 DTO, 없는 경우 null
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public NoticeDTO getNoticeInfo(NoticeDTO pDTO, boolean type) throws Exception {
        log.info("{}.getNoticeInfo Start!", this.getClass().getName());
        log.info("상세 조회할 공지사항 시퀀스: {}", pDTO.getNoticeSeq());

        // 조회수 증가 옵션이 true일 경우, Mapper를 호출하여 조회수를 1 증가시킴
        if (type) {
            log.info("공지사항(시퀀스: {})의 조회수를 증가시킵니다.", pDTO.getNoticeSeq());
            noticeMapper.updateNoticeReadCnt(pDTO);
        } else {
            log.info("조회수를 증가시키지 않습니다.");
        }

        // Mapper를 호출하여 공지사항 상세 정보 조회
        NoticeDTO rDTO = noticeMapper.getNoticeInfo(pDTO);
        if (rDTO != null) {
            log.info("공지사항(시퀀스: {}) 상세 정보 조회를 완료했습니다.", pDTO.getNoticeSeq());
        } else {
            log.warn("공지사항(시퀀스: {}) 정보를 찾을 수 없습니다.", pDTO.getNoticeSeq());
        }

        log.info("{}.getNoticeInfo End!", this.getClass().getName());
        return rDTO;
    }

    /**
     * 새로운 공지사항을 등록합니다.
     *
     * @param pDTO 등록할 공지사항 정보를 담은 DTO
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public void insertNoticeInfo(NoticeDTO pDTO) throws Exception {
        log.info("{}.insertNoticeInfo Start!", this.getClass().getName());
        log.info("새 공지사항 등록 -> 작성자: {}, 제목: {}", pDTO.getUserId(), pDTO.getTitle());

        // Mapper를 호출하여 MongoDB에 데이터를 저장
        noticeMapper.insertNoticeInfo(pDTO);

        log.info("새 공지사항 등록을 완료했습니다.");
        log.info("{}.insertNoticeInfo End!", this.getClass().getName());
    }

    /**
     * 특정 공지사항의 내용을 수정합니다.
     *
     * @param pDTO 수정할 공지사항 정보가 담긴 DTO (noticeSeq 필요)
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public void updateNoticeInfo(NoticeDTO pDTO) throws Exception {
        log.info("{}.updateNoticeInfo Start!", this.getClass().getName());
        log.info("공지사항 수정 -> 시퀀스: {}", pDTO.getNoticeSeq());

        noticeMapper.updateNoticeInfo(pDTO);

        log.info("공지사항 수정을 완료했습니다.");
        log.info("{}.updateNoticeInfo End!", this.getClass().getName());
    }

    /**
     * 특정 공지사항을 삭제합니다.
     *
     * @param pDTO 삭제할 공지사항의 noticeSeq가 포함된 DTO
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public void deleteNoticeInfo(NoticeDTO pDTO) throws Exception {
        log.info("{}.deleteNoticeInfo Start!", this.getClass().getName());
        log.info("공지사항 삭제 -> 시퀀스: {}", pDTO.getNoticeSeq());

        noticeMapper.deleteNoticeInfo(pDTO);

        log.info("공지사항 삭제를 완료했습니다.");
        log.info("{}.deleteNoticeInfo End!", this.getClass().getName());
    }

    @Override
    public void addLike(NoticeDTO pDTO) throws Exception {
        log.info("좋아요 추가: {}", pDTO);
        noticeMapper.updateNoticeLike(pDTO); // Mapper에서 좋아요 추가 구현
    }

    @Override
    public void cancelLike(NoticeDTO pDTO) throws Exception {
        log.info("좋아요 취소: {}", pDTO);
        noticeMapper.cancelNoticeLike(pDTO); // Mapper에서 좋아요 삭제 구현
    }

    @Override
    public int getLikeCount(String noticeSeq) throws Exception {
        return noticeMapper.getNoticeLikeCount(noticeSeq); // Mapper에서 총 좋아요 수 반환
    }

    @Override
    public boolean isLiked(String noticeSeq, String userId) throws Exception {
        return noticeMapper.isNoticeLikedByUser(noticeSeq, userId); // Mapper에서 사용자가 좋아요 했는지 반환
    }


}
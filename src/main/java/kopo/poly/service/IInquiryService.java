package kopo.poly.service;

import kopo.poly.dto.NoticeDTO;

import java.util.List;


public interface IInquiryService {

    /**
     * 공지시항 리스트
     *
     * @return 조회 경과
     */
    List<NoticeDTO> getInquiryList(NoticeDTO pDTO) throws Exception;


    /**
     * 공지시항 상세보기
     *
     * @param pDTO 상세내용 조회할 noticeSeq 값
     * @param type 조회수 증가여부(수정보기는 조회수 증가하지 않음)
     * @return 조회 경과
     */
    NoticeDTO getInquiryInfo(NoticeDTO pDTO, boolean type) throws Exception;

    /**
     * 공지시항 등록
     *
     * @param pDTO 화면에서 입력된 공지사항 입력된 값들
     */
    void insertInquiryInfo(NoticeDTO pDTO) throws Exception;

    /**
     * 공지시항 수정
     *
     * @param pDTO 화면에서 입력된 수정되기 위한 공지사항 입력된 값들
     */
    void updateInquiryInfo(NoticeDTO pDTO) throws Exception;

    /**
     * 공지시항 삭제
     *
     * @param pDTO 삭제할 noticeSeq 값
     */
    void deleteInquiryInfo(NoticeDTO pDTO) throws Exception;

}


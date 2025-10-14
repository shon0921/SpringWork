package kopo.poly.persistance.mongodb;

import com.mongodb.MongoException;
import kopo.poly.dto.NoticeDTO;


import java.util.List;


public interface INoticeMapper {

	//게시판 리스트
	List<NoticeDTO> getNoticeList() throws Exception;
	
	//게시판 글 등록
	void insertNoticeInfo(NoticeDTO pDTO) throws Exception;
	
	//게시판 상세보기
	NoticeDTO getNoticeInfo(NoticeDTO pDTO) throws Exception;

	//게시판 조회수 업데이트
	void updateNoticeReadCnt(NoticeDTO pDTO) throws Exception;
	
	//게시판 글 수정
	void updateNoticeInfo(NoticeDTO pDTO) throws Exception;
	
	//게시판 글 삭제
	void deleteNoticeInfo(NoticeDTO pDTO) throws Exception;

	// 좋아요 추가 (중복 방지)
	void updateNoticeLike(NoticeDTO pDTO) throws Exception;

	// 좋아요 취소
	void cancelNoticeLike(NoticeDTO pDTO) throws Exception;

	int getNoticeLikeCount(String noticeSeq) throws Exception;
	// 총 좋아요 수 조회
	boolean isNoticeLikedByUser(String noticeSeq, String userId) throws Exception; // 해당 사용자가 좋아요 했는지
	
}

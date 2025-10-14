package kopo.poly.persistance.mongodb;

import com.mongodb.MongoException;
import kopo.poly.dto.NoticeDTO;
import kopo.poly.dto.UserDTO;

import java.util.List;


public interface IInquiryMapper {

	//게시판 리스트
	List<NoticeDTO> getInquiryList(NoticeDTO pDTO) throws Exception;
	
	//게시판 글 등록
	void insertInquiryInfo(NoticeDTO pDTO) throws Exception;
	
	//게시판 상세보기
	NoticeDTO getInquiryInfo(NoticeDTO pDTO) throws Exception;

	//게시판 조회수 업데이트
	void updateInquiryReadCnt(NoticeDTO pDTO) throws Exception;

	NoticeDTO getInquiryInfo(NoticeDTO pDTO, boolean type) throws MongoException;

	//게시판 글 수정
	void updateInquiryInfo(NoticeDTO pDTO) throws Exception;
	
	//게시판 글 삭제
	void deleteInquiryInfo(NoticeDTO pDTO) throws Exception;

	// 문의글 삭제 시 댓글 연쇄 삭제 및 회원 탈퇴 시 모든 글 삭제 기능 추가
	void deleteInquiryByUserId(UserDTO pDTO) throws Exception;
	
}

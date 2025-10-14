package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

/**
 * lombok은 코딩을 줄이기 위해 @어노테이션을 통한 자동 코드 완성기능임
 *
 * @Getter => getter 함수를 작성하지 않았지만, 자동 생성
 * @Setter => setter 함수를 작성하지 않았지만, 자동 생성
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoticeDTO {

    private String noticeSeq; // 순번
    private String title; // 제목
    private String noticeYn; // 공지글 여부
    private String contents; // 글 내용
    private String userId; // 작성자 아이디
    private int readCnt; // 조회수
    private String regDt; // 등록일
    private String chgDt; // 수정일
    private String imageUrl; // 이미지 URL
    private int like = 0;    // 추천 갯수

}


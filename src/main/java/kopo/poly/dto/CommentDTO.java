package kopo.poly.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CommentDTO {

    private String commentId;   // _id
    private String noticeSeq;   // 연결된 문의글 번호
    private String userId;      // 댓글 작성자 ID
    private String comment;     // 댓글 내용
    private String regDt;       // 등록일 (문자열: yyyy-MM-dd HH:mm:ss 등)
}

package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import kopo.poly.dto.UserDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IInquiryMapper;
import kopo.poly.persistance.mongodb.IPostMapper;
import kopo.poly.persistance.mongodb.IUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserMapper extends AbstractMongoDBComon implements IUserMapper {

    private final MongoTemplate mongodb;

    private final IInquiryMapper inquiryMapper; // [추가] InquiryMapper 주입
    private final IPostMapper postMapper;       // [추가] PostMapper 주입


    @Override
    public int CheckData(UserDTO pDTO, String colNm) throws MongoException {

        log.info("{}.CheckData Start!", this.getClass().getName());

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Map<String, Object> dataMap = new ObjectMapper().convertValue(pDTO, Map.class);

        // 중복 체크 (userId로 확인)
        Object existingDocument = col.find(new Document("userId", dataMap.get("userId"))).first();

        Object existingDocument2 = col.find(new Document("phoneNumber", dataMap.get("phoneNumber"))).first();

        int res;

        if (existingDocument != null) {
            // 이미 존재하는 경우
            log.info("중복된 userId 값이 존재합니다. .");
            res = 1; // 중복으로 삽입되지 않음
        } else if (existingDocument2 != null) {
            // 중복이 없으면 삽입
            log.info("중복된 phoneNumber 값이 존재합니다. ");
            res = 2; // 성공적으로 삽입되지 않음
        } else {
            log.info("데이터 중복 없음.");
            res = 0; // 성공적으로 삽입됨
        }

        log.info("중복 체크 결과: {}", res);

        log.info("{}.CheckData End!", this.getClass().getName());

        return res;
    }

    @Override
    public int insertData(UserDTO pDTO, String colNm) throws MongoException {

        log.info("{}.insertData Start!", this.getClass().getName());

        // 데이터를 저장할 컬렉션 생성
        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다.", colNm);
        }

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // DTO를 Map으로 변환
        Map<String, Object> dataMap = new ObjectMapper().convertValue(pDTO, Map.class);

        // 아이디 중복 체크 제거 - 무조건 삽입 시도
        col.insertOne(new Document(dataMap));
        log.info("회원가입 데이터 등록 완료");

        log.info("{}.insertData End!", this.getClass().getName());

        return 1; // 삽입 성공
    }

    @Override
    public UserDTO GetLogin(UserDTO pDTO, String colNm) throws MongoException {
        log.info("{}.GetLogin Start!", this.getClass().getName());

        MatchOperation match = Aggregation.match(
                Criteria.where("userId").is(pDTO.getUserId())
                        .and("password").is(pDTO.getPassword())
        );

        // 필요한 필드 다 가져오기 (userId, password, regDt, adminYn 등)
        ProjectionOperation project = Aggregation.project("userId", "password", "regDt", "chgDt", "adminYn", "phoneNumber");

        Aggregation aggregation = Aggregation.newAggregation(match, project);

        AggregationResults<UserDTO> results = mongodb.aggregate(aggregation, colNm, UserDTO.class);

        List<UserDTO> matchedUsers = results.getMappedResults();

        UserDTO user = null;
        if (!matchedUsers.isEmpty()) {
            user = matchedUsers.get(0); // 로그인 성공, 첫번째 사용자 반환
        }

        log.info("{}.GetLogin End!", this.getClass().getName());

        return user; // 성공 시 UserDTO, 실패 시 null 반환
    }

    @Override
    public int forgotPassword1(UserDTO pDTO, String colNm) throws MongoException {

        log.info("{}.forgotPassword1 Start!", this.getClass().getName());

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // userId와 phoneNumber 모두 일치하는 조건
        Document query = new Document("userId", pDTO.getUserId())
                .append("phoneNumber", pDTO.getPhoneNumber());

        Document existingDocument = col.find(query).first();

        int res = (existingDocument != null) ? 1 : 0;   // 1 둘다 일치 0 거짓

        log.info("일치 결과: {}", res);
        log.info("{}.forgotPassword1 End!", this.getClass().getName());

        return res;
    }

    @Override
    public int forgotPassword2(UserDTO pDTO, String colNm) throws MongoException {

        log.info("{}.updatePassword Start!", this.getClass().getName());

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document query = new Document("userId", pDTO.getUserId());

        // 수정할 내용: password + chgDt
        Document updateFields = new Document()
                .append("password", pDTO.getPassword())
                .append("chgDt", pDTO.getChgDt());

        Document update = new Document("$set", updateFields);   // 업데이트

        UpdateResult result = col.updateOne(query, update);

        int res;

        if (result.getMatchedCount() > 0) {
            res = 1; // 업데이트할 문서를 찾았으면 성공
        } else {
            res = 0; // 못 찾았으면 실패
        }

        log.info("비밀번호 변경 결과: {}", res);

        log.info("{}.updatePassword End!", this.getClass().getName());

        return res;
    }

    @Override
    public int deleteData(UserDTO pDTO, String colNm) throws MongoException {

        int res;

        log.info("{}.deleteData Start!", this.getClass().getName());

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // USERID와 PASSWORD가 모두 일치하는 경우 삭제
        Document filter = new Document("userId", pDTO.getUserId())
                .append("password", pDTO.getPassword());

        DeleteResult result = col.deleteOne(filter);

        if (result.getDeletedCount() > 0) {
            log.info("'{}' 회원 정보 삭제 성공", pDTO.getUserId());

            // [수정] 회원 정보 삭제 성공 시, 관련 데이터 삭제 로직 호출
            try {
                // 1. 회원이 작성한 모든 문의글과 관련 댓글 삭제
                inquiryMapper.deleteInquiryByUserId(pDTO);

                // 2. 회원이 저장한 모든 배송조회 내역 삭제
                postMapper.deletePostByUserId(pDTO);

            } catch (Exception e) {
                // 관련 데이터 삭제 중 에러가 발생하더라도 회원 탈퇴 자체는 성공한 것으로 처리합니다.
                // 에러 로그만 남겨서 추후 확인 가능하도록 합니다.
                log.error("'{}' 회원의 관련 데이터(문의, 배송내역) 삭제 중 오류 발생: {}", pDTO.getUserId(), e.getMessage());
            }

            res = 1; // 삭제 성공
        } else {
            log.warn("'{}' 회원 정보 삭제 실패 (비밀번호 불일치 또는 존재하지 않는 회원)", pDTO.getUserId());
            res = 0; // PASSWORD 불일치 또는 사용자 없음
        }

        log.info("{}.deleteData End!", this.getClass().getName());

        return res;
    }

    @Override
    public int checkCurrentPassword(UserDTO pDTO, String colNm) throws MongoException {
        log.info("{}.checkCurrentPassword Start!", this.getClass().getName());

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        Document query = new Document("userId", pDTO.getUserId())
                .append("password", pDTO.getPassword()); // 암호화된 비밀번호

        Document existingDocument = col.find(query).first();

        int res = (existingDocument != null) ? 1 : 0;

        log.info("비밀번호 일치 결과: {}", res);
        log.info("{}.checkCurrentPassword End!", this.getClass().getName());

        return res;
    }

    @Override
    public UserDTO getUserInfo(UserDTO pDTO, String colNm) throws MongoException {
        log.info("{}.getUserInfo Start!", this.getClass().getName());

        Query query = new Query(Criteria.where("userId").is(pDTO.getUserId()));

        UserDTO rDTO = mongodb.findOne(query, UserDTO.class, colNm);

        log.info("{}.getUserInfo End!", this.getClass().getName());

        return rDTO;
    }
}

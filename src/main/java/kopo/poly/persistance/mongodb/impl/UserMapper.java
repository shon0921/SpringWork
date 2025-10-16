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
import kopo.poly.util.DateUtil;
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
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserMapper extends AbstractMongoDBComon implements IUserMapper {

    private final MongoTemplate mongodb;

    private final IInquiryMapper inquiryMapper; // [추가] InquiryMapper 주입
    private final IPostMapper postMapper;       // [추가] PostMapper 주입

    /**
     * 소셜 로그인을 포함한 회원 정보 저장/업데이트를 위한 메서드
     * - 아이디가 존재하지 않으면 새로 삽입 (insert)
     * - 아이디가 존재하면 소셜 정보만 갱신 (update)
     */
    @Override
    public void saveSocialUser(UserDTO pDTO, String colNm) throws Exception {
        log.info("{}.saveSocialUser Start!", this.getClass().getName());

        MongoCollection<Document> col = mongodb.getCollection(colNm);
        Document findQuery = new Document("userId", pDTO.getUserId());
        Document userDoc = col.find(findQuery).first();

        if (userDoc == null) {
            // 사용자가 없으면, 새로 삽입 (일반 회원가입 로직과 유사)
            log.info("New social user. Inserting data...");
            if (super.createCollection(mongodb, colNm)) {
                log.info("{} collection created.", colNm);
            }
            Map<String, Object> dataMap = new ObjectMapper().convertValue(pDTO, Map.class);
            col.insertOne(new Document(dataMap));
            log.info("Social user data inserted successfully.");

        } else {
            // 사용자가 이미 있으면, 정보 업데이트 (provider, chgDt 등)
            log.info("Existing user. Updating social data...");
            Document updateFields = new Document();
            updateFields.append("chgDt", pDTO.getChgDt());
            updateFields.append("provider", pDTO.getProvider()); // provider 정보 갱신
            if (pDTO.getPhoneNumber() != null) {
                updateFields.append("phoneNumber", pDTO.getPhoneNumber()); // 전화번호 갱신
            }

            // ✨ 수정된 부분: totalAmount가 null일 경우 0으로 초기화
            if (userDoc.get("totalAmount") == null) {
                updateFields.append("totalAmount", BigDecimal.ZERO);
            }

            Document updateQuery = new Document("$set", updateFields);
            col.updateOne(findQuery, updateQuery);
            log.info("Social user data updated successfully.");
        }

        log.info("{}.saveSocialUser End!", this.getClass().getName());
    }



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
    public int deleteData(UserDTO pDTO) throws MongoException {

        log.info("{}.deleteData Start!", this.getClass().getName());

        String colNm = "USER"; // 컬렉션 이름 지정
        int res;

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 필터 생성
        Document filter = new Document("userId", pDTO.getUserId());

        // 일반 회원의 경우, 비밀번호까지 필터에 추가
        if (pDTO.getPassword() != null && !pDTO.getPassword().isEmpty()) {
            filter.append("password", pDTO.getPassword());
        }

        // 삭제 실행
        DeleteResult result = col.deleteOne(filter);

        if (result.getDeletedCount() > 0) {
            log.info("'{}' 회원 정보 삭제 성공", pDTO.getUserId());

            // 회원 정보 삭제 성공 시, 관련 데이터(문의, 배송내역) 삭제
            try {
                inquiryMapper.deleteInquiryByUserId(pDTO);
                postMapper.deletePostByUserId(pDTO);
            } catch (Exception e) {
                log.error("'{}' 회원의 관련 데이터 삭제 중 오류 발생: {}", pDTO.getUserId(), e.getMessage());
            }
            res = 1; // 삭제 성공
        } else {
            log.warn("'{}' 회원 정보 삭제 실패 (정보 불일치 또는 존재하지 않는 회원)", pDTO.getUserId());
            res = 0; // 삭제 실패
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

    @Override
    public void updateUserInfo(UserDTO pDTO) {

        log.info(this.getClass().getName() + ".updateUserInfo Start!");

        String colNm = "USER_INFO";

        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(pDTO.getUserId()));

        Update update = new Update();
        update.set("phoneNumber", pDTO.getPhoneNumber()); // 전화번호만 업데이트
        update.set("chgDt", DateUtil.getDateTime("yyyy-MM-dd hh:mm:ss"));

        mongodb.updateMulti(query, update, colNm);

        log.info(this.getClass().getName() + ".updateUserInfo End!");
    }

    @Override
    public UserDTO getUserById(UserDTO pDTO) {
        log.info(this.getClass().getName() + ".getUserById Start!");

        // 조회할 컬렉션
        String colNm = "USER";

        // MongoDB 조회 쿼리
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(pDTO.getUserId()));

        UserDTO rDTO = mongodb.findOne(query, UserDTO.class, colNm);

        log.info(this.getClass().getName() + ".getUserById End!");

        return rDTO;
    }

    @Override
    public void updateTotalAmount(String userId, BigDecimal paidAmount) throws Exception {

        log.info(this.getClass().getName() + ".updateTotalAmount Start!");

        MongoCollection<Document> col = mongodb.getCollection("USER");

        Document filter = new Document("userId", userId);
        Document update = new Document("$inc", new Document("totalAmount", paidAmount));

        col.updateOne(filter, update);

        log.info(this.getClass().getName() + ".updateTotalAmount End!");
    }

}
package kopo.poly.service.impl;

import kopo.poly.dto.MailDTO;
import kopo.poly.dto.UserDTO;
import kopo.poly.persistance.mongodb.IUserMapper;
import kopo.poly.service.IMailService;
import kopo.poly.service.IUserService;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService implements IUserService {

    private final IUserMapper userMapper;
    private final IMailService mailService;

    /**
     * 회원가입 전, 사용자 정보(ID 또는 전화번호)가 이미 존재하는지 확인합니다.
     *
     * @param pDTO 확인할 사용자 정보(userId, phoneNumber 등)를 담은 DTO
     * @return 존재하면 1, 존재하지 않으면 0
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public int register(UserDTO pDTO) throws Exception {
        log.info("{}.register Start!", this.getClass().getName());
        log.info("회원가입 중복 확인 요청 -> 사용자 ID: {}", pDTO.getUserId());

        String colNm = "USER";
        int res = userMapper.CheckData(pDTO, colNm);
        log.info("중복 확인 결과 (0: 없음, 1: 있음): {}", res);

        log.info("{}.register End!", this.getClass().getName());
        return res;
    }

    /**
     * 실제 회원 정보를 DB에 저장하여 회원가입을 완료합니다.
     *
     * @param pDTO 저장할 전체 사용자 정보를 담은 DTO
     * @return 삽입 성공 시 1
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public int register2(UserDTO pDTO) throws Exception {
        log.info("{}.register2 Start!", this.getClass().getName());
        log.info("회원가입 정보 저장 요청 -> 사용자 ID: {}", pDTO.getUserId());

        String colNm = "USER";
        int res = userMapper.insertData(pDTO, colNm);
        log.info("회원가입 정보 저장 완료. 결과: {}", res);

        log.info("{}.register2 End!", this.getClass().getName());
        return res;
    }

    /**
     * 사용자 로그인 처리를 수행합니다.
     *
     * @param pDTO 로그인을 시도하는 사용자의 ID와 비밀번호를 담은 DTO
     * @return 로그인 성공 시 사용자 정보가 담긴 DTO, 실패 시 null
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public UserDTO login(UserDTO pDTO) throws Exception {
        log.info("{}.login Start!", this.getClass().getName());
        log.info("로그인 시도 -> 사용자 ID: {}", pDTO.getUserId());

        String colNm = "USER";
        UserDTO user = userMapper.GetLogin(pDTO, colNm);

        if (user != null) {
            log.info("사용자 '{}' 로그인 성공", pDTO.getUserId());
        } else {
            log.warn("사용자 '{}' 로그인 실패 (ID 또는 비밀번호 불일치)", pDTO.getUserId());
        }

        log.info("{}.login End!", this.getClass().getName());
        return user;
    }

    /**
     * 사용자가 존재하지 않는 경우에만, 입력된 전화번호로 인증번호를 발송합니다. (회원가입 시 사용)
     *
     * @param pDTO 전화번호를 담은 DTO
     * @return 사용자가 존재하면 0, 인증번호 발송 성공 시 6자리 인증번호, 발송 실패 시 -1
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public int PhoneExists(UserDTO pDTO) throws Exception {
        log.info("{}.PhoneExists Start!", this.getClass().getName());
        log.info("휴대폰 인증번호 발송 전 사용자 존재 여부 확인 -> 전화번호: {}", pDTO.getPhoneNumber());

        int res = userMapper.CheckData(pDTO, "USER");
        int authNumber = 0;

        // 사용자가 존재하지 않을 때만 인증번호 발송
        if (res == 0) {
            log.info("사용자가 존재하지 않으므로 인증번호 발송을 시작합니다.");
            authNumber = ThreadLocalRandom.current().nextInt(100000, 999999);
            log.info("생성된 인증번호: {}", authNumber);

            MailDTO dto = new MailDTO();
            dto.setToMail(pDTO.getPhoneNumber());
            dto.setContent("인증번호는 " + authNumber + " 입니다.");

            int mailRes = mailService.doSendMail(dto);
            if (mailRes == 1) {
                log.info("인증번호 발송 성공 -> 수신번호: {}", pDTO.getPhoneNumber());
            } else {
                log.warn("인증번호 발송 실패");
                authNumber = -1; // 실패 시 -1 반환
            }
        } else {
            log.warn("이미 가입된 사용자입니다. 인증번호를 발송하지 않습니다.");
        }

        log.info("{}.PhoneExists End!", this.getClass().getName());
        return authNumber;
    }

    /**
     * 비밀번호 찾기 1단계: 아이디와 전화번호가 일치하는 사용자가 있는지 확인합니다.
     *
     * @param pDTO 확인할 사용자의 아이디와 (암호화된) 전화번호
     * @return 존재하면 1, 아니면 0
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public int forgotNumber1(UserDTO pDTO) throws Exception {
        log.info("{}.forgotNumber1 Start! (비밀번호 찾기 1단계)", this.getClass().getName());
        log.info("사용자 존재 확인 -> ID: {}", pDTO.getUserId());

        String colNm = "USER";
        int res = userMapper.forgotPassword1(pDTO, colNm);
        log.info("사용자 존재 확인 결과 (1: 존재, 0: 없음): {}", res);

        log.info("{}.forgotNumber1 End!", this.getClass().getName());
        return res;
    }

    /**
     * 비밀번호 찾기 2단계: 사용자의 (복호화된) 전화번호로 인증번호를 발송합니다.
     *
     * @param pDTO (암호화된) 전화번호를 담은 DTO
     * @return 발송 성공 시 6자리 인증번호, 실패 시 -1
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public int forgotNumber2(UserDTO pDTO) throws Exception {
        log.info("{}.forgotNumber2 Start! (비밀번호 찾기 2단계)", this.getClass().getName());

        int authNumber = ThreadLocalRandom.current().nextInt(100000, 999999);
        log.info("생성된 인증번호: {}", authNumber);

        // 전화번호 복호화 후 SMS 발송
        String phoneDecrypted = EncryptUtil.decAES128CBC(pDTO.getPhoneNumber());
        log.info("전화번호를 복호화했습니다.");

        MailDTO dto = new MailDTO();
        dto.setToMail(phoneDecrypted);
        dto.setContent("인증번호는 " + authNumber + " 입니다.");

        int res = mailService.doSendMail(dto);
        if (res == 1) {
            log.info("인증번호 발송 성공 -> 수신자(복호화): {}", phoneDecrypted);
        } else {
            log.warn("인증번호 발송 실패");
            authNumber = -1;
        }

        log.info("{}.forgotNumber2 End!", this.getClass().getName());
        return authNumber;
    }

    /**
     * 비밀번호 찾기 3단계: 새로운 비밀번호로 업데이트합니다.
     *
     * @param pDTO 사용자의 ID와 새로운 비밀번호가 담긴 DTO
     * @return 업데이트 성공 시 1
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public int forgotNumber3(UserDTO pDTO) throws Exception {
        log.info("{}.forgotNumber3 Start! (비밀번호 찾기 3단계)", this.getClass().getName());
        log.info("새 비밀번호 업데이트 요청 -> 사용자 ID: {}", pDTO.getUserId());

        String colNm = "USER";
        int res = userMapper.forgotPassword2(pDTO, colNm);
        log.info("새 비밀번호 업데이트 결과: {}", res);

        log.info("{}.forgotNumber3 End!", this.getClass().getName());
        return res;
    }

    /**
     * 사용자 계정을 삭제합니다. Mapper는 내부적으로 관련 데이터를 모두 삭제합니다.
     *
     * @param pDTO 삭제할 사용자의 ID와 비밀번호
     * @return 삭제 성공 시 1, 실패 시 0
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public int accountDelete(UserDTO pDTO) throws Exception {
        log.info("{}.accountDelete Start!", this.getClass().getName());
        log.info("계정 삭제 요청 -> 사용자 ID: {}", pDTO.getUserId());

        int res = userMapper.deleteData(pDTO);
        log.info("계정 삭제 처리 결과 (1: 성공, 0: 실패): {}", res);

        log.info("{}.accountDelete End!", this.getClass().getName());
        return res;
    }

    /**
     * 사용자가 입력한 현재 비밀번호가 DB의 비밀번호와 일치하는지 확인합니다. (비밀번호 변경 시 사용)
     *
     * @param pDTO 확인할 사용자의 ID와 현재 비밀번호
     * @return 일치하면 1, 아니면 0
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public int checkCurrentPassword(UserDTO pDTO) throws Exception {
        log.info("{}.checkCurrentPassword Start!", this.getClass().getName());
        log.info("현재 비밀번호 확인 요청 -> 사용자 ID: {}", pDTO.getUserId());

        String colNm = "USER";
        int res = userMapper.checkCurrentPassword(pDTO, colNm);
        log.info("현재 비밀번호 일치 여부 확인 결과 (1: 일치, 0: 불일치): {}", res);

        log.info("{}.checkCurrentPassword End!", this.getClass().getName());
        return res;
    }

    /**
     * 알림 발송을 위해 사용자 정보를 조회하고, 암호화된 전화번호를 복호화합니다.
     *
     * @param pDTO 조회할 사용자의 userId가 담긴 DTO
     * @return 성공 시 복호화된 전화번호가 포함된 DTO, 실패 시 사용자 정보가 없거나 전화번호가 null인 DTO
     * @throws Exception 처리 중 발생하는 모든 예외
     */
    @Override
    public UserDTO getUserInfoForNotification(UserDTO pDTO) throws Exception {
        log.info("{}.getUserInfoForNotification Start!", this.getClass().getName());
        log.info("알림 발송을 위한 사용자 정보 조회 -> 사용자 ID: {}", pDTO.getUserId());

        // 1. DB에서 암호화된 정보가 담긴 UserDTO를 가져옴
        UserDTO encryptedUser = userMapper.getUserInfo(pDTO, "USER");

        if (encryptedUser != null && encryptedUser.getPhoneNumber() != null) {
            // 2. 전화번호 복호화
            try {
                String decryptedPhone = EncryptUtil.decAES128CBC(encryptedUser.getPhoneNumber());
                // 3. 복호화된 전화번호를 DTO에 다시 설정하여 반환
                encryptedUser.setPhoneNumber(decryptedPhone);
                log.info("사용자 '{}' 정보 조회 및 전화번호 복호화 성공", pDTO.getUserId());
            } catch (Exception e) {
                log.error("사용자 '{}'의 전화번호 복호화 실패", pDTO.getUserId(), e);
                // 복호화 실패 시 알림을 보내면 안 되므로, 전화번호를 null로 처리하여 반환
                encryptedUser.setPhoneNumber(null);
            }
        } else {
            log.warn("사용자 '{}'의 정보를 찾을 수 없거나 전화번호 필드가 비어있습니다.", pDTO.getUserId());
        }

        log.info("{}.getUserInfoForNotification End!", this.getClass().getName());
        return encryptedUser;
    }

    @Override
    public UserDTO getUserInfo(String userId) throws Exception {

        log.info(this.getClass().getName() + ".getUserInfo Start!");

        UserDTO pDTO = UserDTO.builder().userId(userId).build();

        // MongoDB에서 회원 정보 조회
        UserDTO rDTO = userMapper.getUserById(pDTO);

        log.info(this.getClass().getName() + ".getUserInfo End!");

        return rDTO;
    }

    @Override
    public void updateTotalAmount(String userId, BigDecimal paidAmount) throws Exception {
        log.info(this.getClass().getName() + ".updateTotalAmount Start!");
        userMapper.updateTotalAmount(userId, paidAmount);
        log.info(this.getClass().getName() + ".updateTotalAmount End!");
    }
}

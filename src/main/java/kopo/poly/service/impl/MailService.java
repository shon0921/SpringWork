package kopo.poly.service.impl;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.exception.NurigoMessageNotReceivedException;
import net.nurigo.sdk.message.service.DefaultMessageService;

import kopo.poly.dto.MailDTO;
import kopo.poly.service.IMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class MailService implements IMailService {

    /**
     * CoolSMS(Nurigo) API를 사용하여 SMS 문자 메시지를 발송합니다.
     *
     * @param pDTO 발송할 메시지 정보를 담은 DTO (toMail: 수신자 전화번호, content: 메시지 내용)
     * @return 발송 성공 시 1, 실패 시 0
     */
    @Override
    public int doSendMail(MailDTO pDTO) {
        log.info("{}.doSendMail Start!", this.getClass().getName());
        log.info("SMS 발송 요청 수신 -> 수신번호: {}, 내용: '{}'", pDTO.getToMail(), pDTO.getContent());

        int res = 0;  // 1: 성공, 0: 실패

        /*
         * ---------------------------------------------------------------------------------
         * ※ 중요: API 키는 외부에 노출되지 않도록 application.yml 이나 환경 변수에서 관리하는 것이 안전합니다.
         * ---------------------------------------------------------------------------------
         */
        // Nurigo API 초기화 (API 키와 API 시크릿 키 입력)
        DefaultMessageService messageService = NurigoApp.INSTANCE.initialize(
                "NCSKRLMMBDPABBGH",     // API 키
                "JGUVEKAQPHTK5ORLBL0RVILOCNGXYUVL",  // API 시크릿 키
                "https://api.coolsms.co.kr" // CoolSMS API URL
        );

        // 메시지 객체 생성
        Message message = new Message();
        message.setFrom("010-7475-6683");  // 발신번호 (CoolSMS에 등록된 번호)
        message.setTo(pDTO.getToMail());     // 수신번호
        message.setText(pDTO.getContent());  // 메시지 내용

        try {
            log.info("Nurigo API를 통해 메시지 전송을 시도합니다.");
            // 메시지 전송
            messageService.send(message);
            res = 1; // 성공 플래그 설정
            log.info("SMS 발송 성공! 수신번호: {}", pDTO.getToMail());

        } catch (NurigoMessageNotReceivedException exception) {
            // Nurigo API에서 정의한 발송 실패 예외 처리
            log.error("SMS 발송 실패 (NurigoMessageNotReceivedException): {}", exception.getMessage());
            log.error(" - 실패한 메시지 목록: {}", exception.getFailedMessageList());

        } catch (Exception exception) {
            // 그 외 모든 예외 처리
            log.error("SMS 발송 중 알 수 없는 오류가 발생했습니다.", exception);
        }

        log.info("SMS 발송 처리 종료. 결과: {}", (res == 1) ? "성공" : "실패");
        log.info("{}.doSendMail End!", this.getClass().getName());

        return res;
    }
}

package kopo.poly.scheduler;

import kopo.poly.dto.MailDTO;
import kopo.poly.dto.PostDTO;
import kopo.poly.dto.TrackingDTO;
import kopo.poly.dto.UserDTO;
import kopo.poly.service.IMailService;
import kopo.poly.service.IPostService;
import kopo.poly.service.ITrackingService;
import kopo.poly.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.time.format.DateTimeParseException;


/**
 * 배송 상태를 주기적으로 갱신하는 스케줄러 클래스.
 * Spring의 @Scheduled 어노테이션을 사용하여 백그라운드에서 동작합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryStatusScheduler {

    // 필요한 서비스들을 의존성 주입(DI) 받습니다.
    private final IPostService postService;         // 배송 정보 관련 서비스
    private final ITrackingService trackingService; // 외부 배송 추적 API 연동 서비스
    private final IUserService userService;         // 사용자 정보 관련 서비스
    private final IMailService mailService;         // 이메일 또는 SMS 발송 서비스

    // 외부 API에서 사용하는 ISO 8601 형식의 날짜/시간 문자열을 파싱하기 위한 포맷터
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    // 데이터베이스에 저장할 날짜/시간 형식("yyyy-MM-dd HH:mm")을 위한 포맷터
    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 주기적으로 실행되어 배송 상태를 일괄 갱신하는 메서드.
     * cron 표현식 "0 0/2 * * * *"에 따라 2분마다 실행됩니다. (원래는 30분마다)
     */
    @Scheduled(cron = "0 0/2 * * * *") // 30분마다 실행 임시 2분 수정
    public void updateDeliveryStatuses() {
        log.info("==================== 배송 상태 일괄 갱신 스케줄러 시작 ====================");

        // 1. DB에서 아직 배송 완료되지 않은 모든 배송 건을 조회합니다.
        List<PostDTO> deliveryLogs = postService.getAllUncompletedLogs();
        log.info("갱신 대상 배송 건수: {}건", deliveryLogs.size());

        // 갱신할 데이터가 없으면 스케줄러를 즉시 종료합니다.
        if (deliveryLogs.isEmpty()) {
            log.info("갱신할 배송 정보가 없습니다.");
            log.info("==================== 배송 상태 일괄 갱신 스케줄러 종료 ====================");
            return;
        }

        // 2. 조회된 각 배송 건에 대해 최신 상태를 확인하고 업데이트합니다.
        for (PostDTO savedLog : deliveryLogs) {
            try {
                // 외부 배송 추적 API를 호출하여 최신 배송 정보를 가져옵니다.
                TrackingDTO latestInfo = trackingService.getTrackingInfo(savedLog.getCarrierId(), savedLog.getTrackingNumber());

                // API 응답이 없거나 상태 정보가 없으면 다음 건으로 넘어갑니다.
                if (latestInfo == null || latestInfo.getState() == null) {
                    continue;
                }

                // API 응답에서 새로운 상태 ID, 상태 텍스트, 마지막 배송 시간을 추출합니다.
                String newStateId = latestInfo.getState().getId();
                String newStateText = latestInfo.getState().getText();
                String newLastDeliveryTime = null;

                // 배송 진행 정보(progresses)가 존재하면, 가장 마지막 시간 기록을 파싱합니다.
                if (latestInfo.getProgresses() != null && !latestInfo.getProgresses().isEmpty()) {
                    // 가장 마지막 진행 단계의 시간을 가져옵니다.
                    String lastProgressTimeStr = latestInfo.getProgresses().get(latestInfo.getProgresses().size() - 1).getTime();
                    if (StringUtils.hasText(lastProgressTimeStr)) {
                        try {
                            // ISO 형식의 시간을 OffsetDateTime 객체로 파싱합니다.
                            OffsetDateTime odt = OffsetDateTime.parse(lastProgressTimeStr, ISO_OFFSET_DATE_TIME_FORMATTER);
                            // DB 저장 형식으로 변환합니다.
                            newLastDeliveryTime = odt.format(DB_DATE_TIME_FORMATTER);
                        } catch (DateTimeParseException e) {
                            log.error("API 응답의 시간 형식 파싱 실패", e);
                        }
                    }
                }

                // DB에 저장된 값과 API에서 가져온 최신 값을 비교하여 변경 여부를 확인합니다.
                boolean isStateChanged = !Objects.equals(savedLog.getStateId(), newStateId);
                boolean isTimeChanged = newLastDeliveryTime != null && !Objects.equals(savedLog.getLastDeliveryTime(), newLastDeliveryTime);

                boolean notificationSent = false; // 알림 발송 성공 여부 플래그

                // 3. 배송 상태가 변경되었을 경우, 사용자에게 알림을 발송합니다.
                if (isStateChanged) {
                    notificationSent = sendNotificationOnStatusChange(savedLog, newStateId);
                }

                // 4. 상태, 시간, 또는 알림 발송 여부(N->Y) 중 하나라도 변경된 경우에만 DB를 업데이트합니다.
                if (isStateChanged || isTimeChanged || notificationSent) {
                    log.info("DB 업데이트 필요! 사용자: {}, 운송장번호: {}", savedLog.getUserId(), savedLog.getTrackingNumber());
                    if (isStateChanged) log.info(" -> 상태 ID 변경: [{}] -> [{}]", savedLog.getStateId(), newStateId);
                    if (isTimeChanged) log.info(" -> 시간 변경: [{}] -> [{}]", savedLog.getLastDeliveryTime(), newLastDeliveryTime);
                    if (notificationSent) log.info(" -> 알림 발송 플래그가 'Y'로 변경됨");

                    // DB 업데이트를 위한 DTO를 생성하고 새로운 정보로 채웁니다.
                    PostDTO updateDTO = new PostDTO();
                    updateDTO.setUserId(savedLog.getUserId());
                    updateDTO.setTrackingNumber(savedLog.getTrackingNumber());
                    updateDTO.setStateId(newStateId);
                    updateDTO.setStateText(newStateText);
                    updateDTO.setLastDeliveryTime(newLastDeliveryTime);

                    // 특정 상태('배달 출발', '배달 완료')에 대해 알림 발송에 성공했다면,
                    // 해당 상태의 알림 발송 플래그를 true('Y')로 설정하여 중복 발송을 방지합니다.
                    if (newStateId.equals("out_for_delivery") && notificationSent) {
                        updateDTO.setNotificationSentForOutForDelivery(true);
                    }
                    if (newStateId.equals("delivered") && notificationSent) {
                        updateDTO.setNotificationSentForDelivered(true);
                    }

                    // 서비스 레이어를 통해 DB에 최종적으로 업데이트를 수행합니다.
                    postService.updateLogByScheduler(updateDTO);
                }
                // 외부 API에 대한 과도한 요청을 방지하기 위해 각 요청 사이에 0.5초의 딜레이를 줍니다.
                Thread.sleep(500);

            } catch (InterruptedException e) {
                // 스레드가 중단될 경우, 현재 스레드의 중단 상태를 다시 설정하고 경고 로그를 남깁니다.
                Thread.currentThread().interrupt();
                log.warn("배송 상태 갱신 스레드가 중단되었습니다.", e);
            } catch (Exception e) {
                // 그 외 모든 예외에 대해 로그를 남겨 어떤 배송 건에서 문제가 발생했는지 추적할 수 있도록 합니다.
                log.error("ID: {}, 운송장번호: {} 처리 중 예외 발생", savedLog.getUserId(), savedLog.getTrackingNumber(), e);
            }
        }
        log.info("==================== 배송 상태 일괄 갱신 스케줄러 종료 ====================");
    }

    /**
     * 배송 상태 변경 시 사용자에게 알림을 보내고, 성공 여부를 반환하는 메서드.
     *
     * @param savedLog   DB에 저장되어 있던 기존 배송 정보 DTO
     * @param newStateId API를 통해 새로 확인된 배송 상태 ID
     * @return 알림을 성공적으로 보냈으면 true, 실패했거나 보낼 필요가 없으면 false를 반환합니다.
     */
    private boolean sendNotificationOnStatusChange(PostDTO savedLog, String newStateId) {
        try {
            String messageTemplate = null; // 발송할 메시지 템플릿

            // [핵심 로직] 새로운 상태 ID에 따라 알림을 보낼지 결정합니다.
            // DB에 저장된 알림 발송 여부 플래그를 확인하여 중복 발송을 방지합니다.
            switch (newStateId) {
                case "out_for_delivery": // '배달 출발' 상태일 경우
                    // '배달 출발' 알림을 아직 보낸 적이 없다면 (플래그가 false, 즉 DB 값이 'N'이라면)
                    if (!savedLog.isNotificationSentForOutForDelivery()) {
                        messageTemplate = "[%s]님, %s(%s) 상품이 배달을 시작합니다.";
                    }
                    break;
                case "delivered": // '배달 완료' 상태일 경우
                    // '배달 완료' 알림을 아직 보낸 적이 없다면 (플래그가 false, 즉 DB 값이 'N'이라면)
                    if (!savedLog.isNotificationSentForDelivered()) {
                        messageTemplate = "[%s]님, %s(%s) 상품의 배달이 완료되었습니다.";
                    }
                    break;
                default:
                    // '배달 출발' 또는 '배달 완료'가 아닌 다른 상태 변경은 알림 대상이 아니므로 false를 반환합니다.
                    return false;
            }

            // 보낼 메시지가 결정되지 않았다면 (이미 보냈거나, 알림 대상이 아니거나) 함수를 종료합니다.
            if (messageTemplate == null) {
                return false;
            }

            // 알림 발송을 위해 사용자 정보를 조회합니다.
            UserDTO pDTO = new UserDTO();
            pDTO.setUserId(savedLog.getUserId());
            UserDTO user = userService.getUserInfoForNotification(pDTO);

            // 사용자 정보나 연락처가 없으면 알림을 보낼 수 없으므로 경고 로그를 남기고 종료합니다.
            if (user == null || !StringUtils.hasText(user.getPhoneNumber())) {
                log.warn("사용자 '{}'의 정보 또는 연락처가 없어 알림을 보낼 수 없습니다.", savedLog.getUserId());
                return false;
            }

            // 메시지 내용 포맷팅
            String nickname = user.getUserId();
            String decryptedPhoneNumber = user.getPhoneNumber(); // 복호화된 전화번호
            String content = String.format(messageTemplate, nickname, savedLog.getCarrierName(), savedLog.getTrackingNumber());

            // MailDTO를 생성하고 SMS(또는 이메일) 발송에 필요한 정보를 설정합니다.
            MailDTO mailDTO = new MailDTO();
            mailDTO.setToMail(decryptedPhoneNumber); // 수신자 전화번호
            mailDTO.setContent(content);             // 메시지 내용

            // 메일(SMS) 서비스를 호출하여 알림을 발송합니다.
            mailService.doSendMail(mailDTO);
            log.info("SMS 알림 발송 완료 -> 사용자: {}, 내용: {}", savedLog.getUserId(), content);

            // 알림 발송에 성공했으므로 true를 반환합니다.
            // 이 반환 값은 호출부(updateDeliveryStatuses)에서 DB의 알림 플래그를 업데이트하는 데 사용됩니다.
            return true;

        } catch (Exception e) {
            // 알림 발송 과정에서 예외가 발생하면 에러 로그를 남기고 실패(false)를 반환합니다.
            log.error("사용자 '{}'에게 알림 발송 중 예외 발생", savedLog.getUserId(), e);
            return false;
        }
    }
}
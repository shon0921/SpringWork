package kopo.poly.controller;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.MailDTO;
import kopo.poly.dto.ReceiptDTO;
import kopo.poly.dto.UserDTO;
import kopo.poly.service.IMailService;
import kopo.poly.service.IReceiptService;
import kopo.poly.service.IUserService;
import kopo.poly.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import kopo.poly.util.EncryptUtil;

@Slf4j
@Controller
@RequestMapping("/portone")
@RequiredArgsConstructor
public class PortoneController {

    private final IUserService userService;
    private final IMailService mailService;
    private final IReceiptService receiptService; // IReceiptService 주입

    @Value("${portone.api_key}")
    private String apiKey;

    @Value("${portone.api_secret}")
    private String apiSecret;

    private IamportClient iamportClient;

    @PostConstruct
    public void init() {
        this.iamportClient = new IamportClient(apiKey, apiSecret);
    }

    /**
     * 결제 페이지를 보여주는 메서드 (HTML 반환)
     */
    @GetMapping("/payment")
    public String showPaymentPage(HttpSession session, ModelMap model) throws Exception {
        String userId = (String) session.getAttribute("userId");
        log.info("session user_id: {}", userId);

        if (userId == null) {
            return "redirect:/login";
        }

        UserDTO rDTO = Optional.ofNullable(userService.getUserInfo(userId)).orElse(new UserDTO());
        model.addAttribute("buyerName", userId);
        model.addAttribute("tel", EncryptUtil.decAES128CBC(rDTO.getPhoneNumber()));

        return "boost/payment";
    }

    /**
     * 로그인 사용자 정보를 JSON으로 반환
     * Ajax에서 가져와 JS 변수로 사용
     */
    @ResponseBody
    @GetMapping("/paymentInfo")
    public Map<String, String> getPaymentInfo(HttpSession session) throws Exception {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        UserDTO rDTO = Optional.ofNullable(userService.getUserInfo(userId)).orElse(new UserDTO());

        Map<String, String> result = new HashMap<>();
        result.put("userId", userId);
        result.put("phone", EncryptUtil.decAES128CBC(rDTO.getPhoneNumber())); // 복호화 후 반환
        return result;
    }

    /**
     * 클라이언트로부터 imp_uid를 받아 결제 정보를 검증하고, 성공 시 SMS 발송 및 영수증 저장
     */
    @ResponseBody
    @PostMapping("/verify/{imp_uid}")
    public IamportResponse<Payment> verifyPayment(@PathVariable("imp_uid") String imp_uid, HttpSession session)
            throws IamportResponseException, IOException {
        log.info("서버에서 결제 검증을 시작합니다. imp_uid: {}", imp_uid);
        IamportResponse<Payment> paymentResponse = iamportClient.paymentByImpUid(imp_uid);

        // 결제 검증 성공 시
        if (paymentResponse.getResponse() != null) {
            String userId = (String) session.getAttribute("userId");
            if (userId != null) {
                try {
                    // 1. 사용자 정보 조회
                    UserDTO pDTO = UserDTO.builder().userId(userId).build();
                    UserDTO rDTO = userService.getUserInfoForNotification(pDTO);

                    if (rDTO != null && rDTO.getPhoneNumber() != null) {
                        BigDecimal paidAmount = paymentResponse.getResponse().getAmount();
                        String currentDate = DateUtil.getDateTime("yyyy-MM-dd HH:mm:ss");
                        String cardName = paymentResponse.getResponse().getCardName(); // 카드사 정보 가져오기

                        // 2. 영수증 정보 DTO 생성
                        ReceiptDTO receiptDTO = new ReceiptDTO();
                        receiptDTO.setUserId(userId);
                        receiptDTO.setMoney(paidAmount.toString());
                        receiptDTO.setDate(currentDate);
                        receiptDTO.setCardName(cardName); // 카드사 정보 설정

                        // 3. 영수증 정보 DB에 저장하고, 'seq'가 포함된 DTO를 반환받음
                        ReceiptDTO savedReceipt = receiptService.insertReceipt(receiptDTO);
                        log.info("영수증 정보 DB 저장 성공 (Receipt Seq: {})", savedReceipt.getReceiptSeq());

                        // 4. 누적 결제 금액 업데이트
                        userService.updateTotalAmount(userId, paidAmount);
                        log.info("누적 결제 금액 업데이트 성공");

                        // --- 🚨 5. SMS 내용에 카드사 정보를 추가 ---
                        String userPhoneNumber = rDTO.getPhoneNumber();
                        String msg = "[결제완료] 영수증 번호 : " + savedReceipt.getReceiptSeq() + "\n"
                                + "결제수단 : " + cardName + "\n"
                                + userId + "님 " + paidAmount + "원이 결제되었습니다.\n"
                                + "결제일시: " + currentDate;


                        // --- 🚨 6. Builder 대신 new와 setter로 MailDTO 생성 ---
                        MailDTO mailDTO = new MailDTO();
                        mailDTO.setToMail(userPhoneNumber);
                        mailDTO.setContent(msg);

                        mailService.doSendMail(mailDTO);
                        log.info("결제 완료 SMS 발송 성공 -> 수신자: {}", userPhoneNumber);
                    }
                } catch (Exception e) {
                    log.error("SMS 발송 또는 DB 작업 중 오류 발생: {}", e.getMessage(), e);
                }
            }
        }

        return paymentResponse;
    }
}
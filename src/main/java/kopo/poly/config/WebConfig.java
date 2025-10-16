package kopo.poly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // forward: 뒤의 경로는 /static/ 폴더 내부의 실제 파일 경로입니다.

        // 루트 URL 설정
        registry.addViewController("/").setViewName("forward:/html/index.html");

        // /html
        registry.addViewController("/forgotpassword").setViewName("forward:/html/forgotpassword.html");
        registry.addViewController("/forgotpassword2").setViewName("forward:/html/forgotpassword2.html");
        registry.addViewController("/forgotpassword3").setViewName("forward:/html/forgotpassword3.html");
        registry.addViewController("/guestdeliverytracking").setViewName("forward:/html/guestdeliverytracking.html");
        registry.addViewController("/login").setViewName("forward:/html/login.html");
        registry.addViewController("/register").setViewName("forward:/html/register.html");
        registry.addViewController("/register2").setViewName("forward:/html/register2.html");
        registry.addViewController("/trackingResult").setViewName("forward:/html/trackingResult.html");

        // /html/main
        registry.addViewController("/main/myaccount").setViewName("forward:/html/main/myaccount.html");
        registry.addViewController("/main/changepassword").setViewName("forward:/html/main/changepassword.html");
        registry.addViewController("/main/accountdelete").setViewName("forward:/html/main/accountdelete.html");
        registry.addViewController("/main/title").setViewName("forward:/html/main/title.html");

        // /html/main/inquiry
        registry.addViewController("/main/inquiry/inquiryDetail").setViewName("forward:/html/main/inquiry/inquiryDetail.html");
        registry.addViewController("/main/inquiry/inquiryEditInfo").setViewName("forward:/html/main/inquiry/inquiryEditInfo.html");
        registry.addViewController("/main/inquiry/inquiryInfo").setViewName("forward:/html/main/inquiry/inquiryInfo.html");
        registry.addViewController("/main/inquiry/inquiryList").setViewName("forward:/html/main/inquiry/inquiryList.html");
        registry.addViewController("/main/inquiry/inquiryReg").setViewName("forward:/html/main/inquiry/inquiryReg.html");

        // /html/main/notice
        registry.addViewController("/main/notice/noticeDetail").setViewName("forward:/html/main/notice/noticeDetail.html");
        registry.addViewController("/main/notice/noticeEditInfo").setViewName("forward:/html/main/notice/noticeEditInfo.html");
        registry.addViewController("/main/notice/noticeInfo").setViewName("forward:/html/main/notice/noticeInfo.html");
        registry.addViewController("/main/notice/noticeList").setViewName("forward:/html/main/notice/noticeList.html");
        registry.addViewController("/main/notice/noticeReg").setViewName("forward:/html/main/notice/noticeReg.html");

        // /html/main/post
        registry.addViewController("/main/post/deliverytracking").setViewName("forward:/html/main/post/deliverytracking.html");
        registry.addViewController("/main/post/postList").setViewName("forward:/html/main/post/postList.html");
        registry.addViewController("/main/post/trackingResult2").setViewName("forward:/html/main/post/trackingResult2.html");

        registry.addViewController("/main/boost/payment").setViewName("forward:/html/main/boost/payment.html");

    }
}
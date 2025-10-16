package kopo.poly.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

// 필터 로직을 담고 있는 내부 클래스
class WwwRedirectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String host = req.getHeader("Host");

        // "Host" 헤더가 "deliveryhub.kr"인 경우 www. 붙여서 리디렉션
        if (host != null && host.equals("deliveryhub.kr")) {
            String newUrl = "https://" + "www." + host + req.getRequestURI(); // 프로토콜을 https로 고정
            if (req.getQueryString() != null) {
                newUrl += "?" + req.getQueryString();
            }
            res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // 301 영구 이동
            res.setHeader("Location", newUrl);
            return; // 리디렉션 후에는 체인의 다음 필터를 실행하지 않음
        }

        chain.doFilter(request, response); // 그 외의 경우는 정상적으로 요청 처리
    }
}

// 필터를 Spring에 등록하는 설정 클래스
@Configuration
public class WwwRedirectConfig {

    @Bean
    public FilterRegistrationBean<WwwRedirectFilter> wwwRedirectFilterRegistration() {
        FilterRegistrationBean<WwwRedirectFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new WwwRedirectFilter()); // 위에서 정의한 필터 클래스를 등록
        registrationBean.addUrlPatterns("/*"); // 모든 URL 패턴에 대해 필터를 적용
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 가장 먼저 실행되는 필터로 순서 설정

        return registrationBean;
    }
}
package kopo.poly.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WwwRedirectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String host = req.getHeader("Host");

        if (host != null && host.equals("deliveryhub.kr")) {
            String newUrl = req.getScheme() + "://www." + host + req.getRequestURI();
            if (req.getQueryString() != null) {
                newUrl += "?" + req.getQueryString();
            }
            res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // 301 리디렉션
            res.setHeader("Location", newUrl);
            return;
        }

        chain.doFilter(request, response);
    }
}
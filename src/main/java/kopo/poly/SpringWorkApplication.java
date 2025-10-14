package kopo.poly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling   // 스케줄링 사용
@EnableFeignClients // 오픈 페이전? 사용
@SpringBootApplication
public class SpringWorkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringWorkApplication.class, args);
    }

}

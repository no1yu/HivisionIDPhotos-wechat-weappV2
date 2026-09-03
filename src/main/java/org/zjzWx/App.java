package org.zjzWx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        System.out.println("--------------------------------------------------------");
        System.out.println("欢迎使用证件照伴侣V2服务");
    }
}

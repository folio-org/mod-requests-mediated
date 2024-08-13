package org.folio.mr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class RequestsMediatedApplication {

  public static void main(String[] args) {
    SpringApplication.run(RequestsMediatedApplication.class, args);
  }

}

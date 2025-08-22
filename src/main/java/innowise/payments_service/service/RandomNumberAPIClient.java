package innowise.payments_service.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "random-number-api-client", url = "${external-api.random-number.host}")
public interface RandomNumberAPIClient {
    @GetMapping(value = "${external-api.random-number.url}")
    String getRandomNumber();

    default Integer getRandomNumberAsInteger() {
        return Integer.parseInt(getRandomNumber().trim());
    }
}
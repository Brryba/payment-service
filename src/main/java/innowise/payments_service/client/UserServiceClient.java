package innowise.payments_service.client;

import innowise.payments_service.dto.user.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Component
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {
    @GetMapping("/api/user/{userId}")
    UserResponseDto getUserById(@PathVariable("userId") Long userId);
}

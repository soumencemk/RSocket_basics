package com.soumen.rsocket.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);

    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest {
    private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ClientHealthState {
    private boolean healthy;
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse {
    private String message;
}

@Controller
@RequiredArgsConstructor
@Log4j2
class GreetingController {

    private final GreetingService greetingService;


    @MessageMapping("greetings")
    Flux<GreetingResponse> greet(
            @AuthenticationPrincipal Mono<UserDetails> authenticatedUser,
            RSocketRequester currentClientRsocket) {
        log.info("Request Received ");
        return authenticatedUser
                .map(ud -> ud.getUsername())
                .map(userName -> new GreetingRequest(userName))
                .flatMapMany(g -> this.greet(currentClientRsocket, g));

    }

    private Flux<GreetingResponse> greet(
            RSocketRequester currentClientRsocket,
            GreetingRequest request) {

        Flux<ClientHealthState> health = currentClientRsocket.route("health").retrieveFlux(ClientHealthState.class).filter(chs -> !chs.isHealthy());
        Flux<GreetingResponse> greet = greetingService.greet(request);
        return greet.takeUntilOther(health);
    }


}

@Service
class GreetingService {

    Flux<GreetingResponse> greet(GreetingRequest request) {
        return Flux.fromStream(Stream.generate(() -> new GreetingResponse("Hello " + request.getName() + " @ " + Instant.now().toString())))
                .take(100)
                .delayElements(Duration.ofSeconds(1));
    }

}


@Configuration
class SecurityConfig {

    @Bean
    RSocketMessageHandler rSocketMessageHandler(RSocketStrategies strategies) {
        var rmh = new RSocketMessageHandler();
        rmh.getArgumentResolverConfigurer().addCustomResolver(new AuthenticationPrincipalArgumentResolver());
        rmh.setRSocketStrategies(strategies);
        return rmh;
    }

    @Bean
    PayloadSocketAcceptorInterceptor authorisation(RSocketSecurity rSocketSecurity) {
        return rSocketSecurity
                .simpleAuthentication(Customizer.withDefaults())
                .build();

    }

    @Bean
    MapReactiveUserDetailsService authentication() {
        return new MapReactiveUserDetailsService(
                User.withDefaultPasswordEncoder()
                        .username("soumen")
                        .password("soumen")
                        .roles("USER")
                        .build());
    }

}

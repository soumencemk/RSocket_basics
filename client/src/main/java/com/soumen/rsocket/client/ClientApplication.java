package com.soumen.rsocket.client;

import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.WellKnownMimeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder;
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.stream.Stream;

@SpringBootApplication
@Log4j2
public class ClientApplication {

    private final MimeType mimeType = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.getString());
    private final UsernamePasswordMetadata usernamePasswordMetadata =
            new UsernamePasswordMetadata("soumen", "soumen");

    @Bean
    RSocketStrategiesCustomizer rSocketStrategiesCustomizer() {
        return strategies -> strategies.encoder(new SimpleAuthenticationEncoder());
    }


    @Bean
    ApplicationListener<ApplicationReadyEvent> ready(RSocketRequester rSocketRequester) {
        return event -> {
            rSocketRequester
                    .route("greetings")
                    .data(new GreetingRequest("soumen"))
                    .retrieveFlux(GreetingResponse.class)
                    .subscribe(log::info);
        };
    }

    @Bean
    SocketAcceptor socketAcceptor(RSocketStrategies strategies, HealthController healthController) {
        return RSocketMessageHandler.responder(strategies, healthController);
    }

    @Bean
    RSocketRequester rSocketRequester(RSocketRequester.Builder builder, SocketAcceptor socketAcceptor) {
        return builder
                .setupMetadata(this.usernamePasswordMetadata, this.mimeType)
                .rsocketConnector(c -> c.acceptor(socketAcceptor))
                .tcp("localhost", 8888);
    }

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
        System.in.read();
    }

}

@Controller
class HealthController {

    @MessageMapping("health")
    Flux<ClientHealthState> health() {
        return Flux.fromStream(Stream.generate(() -> new ClientHealthState(Math.random() > 0.75)))
                .delayElements(Duration.ofSeconds(1));
    }
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
class GreetingRequest {
    private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse {
    private String message;
}

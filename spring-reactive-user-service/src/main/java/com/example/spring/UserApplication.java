package com.example.spring;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Log4j2
@SpringBootApplication
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

    @Bean
    ConnectionFactory connectionFactory() {
        H2ConnectionConfiguration config = H2ConnectionConfiguration
                .builder()
                .url("~/.h2/spring.reactive")
                .username("sa")
                .build();
        return new H2ConnectionFactory(config);
    }

}

@AllArgsConstructor
@RestController
@RequestMapping("/")
class UserRestController {

    private final ConnectionFactory connectionFactory;

    private static final String INSERT_STATEMENT = "insert into t_user(username, password) values(?, ?)";

    private static final String FIND_STATEMENT = "select * from t_user limit ?, ?";

    private static final String FIND_BY_ID = "select * from t_user where id = ?";

    @GetMapping("/greeting")
    Mono<String> greeting(String name) {
        return Mono.just("Hi " + name);
    }

    @PostMapping("/user/register")
    Mono<String> register(String username, String password) {
        return Mono.from(connectionFactory.create())
                .flatMap(a -> Mono.from(a.createStatement(INSERT_STATEMENT).bind(0, username).bind(1, password).execute()))
                .then(Mono.just("done"));
    }

    @GetMapping("/user/list")
    Flux<User> list(int offset, int size) {
        return Flux.from(connectionFactory.create())
                .flatMap(a -> a.createStatement(FIND_STATEMENT).bind(0, offset).bind(1, size).execute())
                .flatMap(a -> a.map((r, m) -> {
                    Long id = Objects.requireNonNull(r.get("ID", Integer.class)).longValue();
                    String username = r.get("USERNAME", String.class);
                    String password = r.get("PASSWORD", String.class);
                    return new User(id, username, password);
                }));
    }

    @GetMapping("/user/i/{id}")
    Mono<User> user(@PathVariable("id") Long id) {
        return Mono.from(connectionFactory.create())
                .flatMap(a -> Mono.from(a.createStatement(FIND_BY_ID).bind(0, id).execute()))
                .flatMap(a -> Mono.from(a.map((r, m) -> {
                    Long userId = r.get("ID", Integer.class).longValue();
                    String username = r.get("USERNAME", String.class);
                    String password = r.get("PASSWORD", String.class);
                    return new User(userId, username, password);
                })));
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("t_user")
class User {
    @Id
    private Long id;
    private String username;
    private String password;
}

//interface UserRepository extends ReactiveCrudRepository<User, Long> {
//
//}
//
//@Configuration
//@EnableR2dbcRepositories
//class R2dbcConfiguration extends AbstractR2dbcConfiguration {
//
////    @Override
////    public ConnectionFactory connectionFactory() {
////        String url = "mysql://username:password@localhost:3306/moment";
////        return new JasyncConnectionFactory(
////                new MySQLConnectionFactory(URLParser.INSTANCE.parseOrDie(url, StandardCharsets.UTF_8))
////        );
////    }
//
//    @Bean
//    @Override
//    public ConnectionFactory connectionFactory() {
//        H2ConnectionConfiguration config = H2ConnectionConfiguration
//                .builder()
//                .url("~/.h2/spring.reactive")
//                .username("sa")
//                .build();
//        return new H2ConnectionFactory(config);
//    }
//
//}
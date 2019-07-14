package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbcx.JdbcConnectionPool;

import javax.sql.DataSource;
import java.util.List;

@Slf4j
public class UserVerticle extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(UserVerticle.class.getName(), ar -> {
            if (ar.succeeded()) {
                log.info("user service deployed!");
            } else {
                log.error("user service deploy failed.", ar.cause());
            }
        });
    }

    private JDBCClient jdbcClient;
    private DataSource dataSource;

    @Override
    public void start() throws Exception {
        this.dataSource = JdbcConnectionPool.create("jdbc:h2:~/.h2/vertx.reactive", "sa", "");
        this.jdbcClient = JDBCClient.create(vertx, dataSource);

        vertx.fileSystem().readFile("schema.sql", buffer -> jdbcClient.getConnection(conn -> {
            SQLConnection connection = conn.result();
            connection.update(buffer.result().toString(), res -> connection.close());
        }));

        Router router = Router.router(vertx);

        router.route("/greeting")
                .handler(this::handleGreeting);

        router.route("/user/register")
                .handler(this::handleUserRegister);

        router.route("/user/list")
                .handler(this::handleListUsers);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8081, ar -> {
                    if (ar.succeeded()) {
                        log.info("create http server at port 8081");
                    } else {
                        log.info("create http server listen failed");
                    }
                });
    }

    private void handleGreeting(RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();
        String name = request.getParam("name");
        log.info("begin say greeting to user {}", name);
        HttpServerResponse response = routingContext.response();
        response.end("Hi " + name, ar -> log.info("end say greeting！"));
    }

    private void handleListUsers(RoutingContext routingContext) {

        Future<List<JsonObject>> future = Future.future();
        HttpServerRequest request = routingContext.request();
        JsonArray params = new JsonArray()
                .add(request.getParam("offset"))
                .add(request.getParam("size"));

        jdbcClient.getConnection(ar -> {
            SQLConnection conn = ar.result();
            conn.queryWithParams(FIND_ALL, params, result -> {
                future.complete(result.result().getRows());
                conn.close();
            });
        });

        // @formatter:off
        future
                /*.map(rows -> rows.stream()
                                .map(User::map)
                                .collect(Collectors.toList()))*/
                .setHandler(e -> routingContext
                        .response()
                        .putHeader("content-type", "application/json")
                        .end(e.result().toString()));
        // @formatter:on
    }

    private void handleUserRegister(RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();

        JsonArray params = new JsonArray()
                .add(request.getParam("username"))
                .add(request.getParam("password"));

        jdbcClient.getConnection(ar -> {
            SQLConnection conn = ar.result();
            conn.updateWithParams(INSERT_STATEMENT, params, result -> {
                routingContext.response().end("done");
                conn.close();
            });
        });
    }

    private static final String INSERT_STATEMENT = "insert into t_user(username, password) values(?, ?)";

    private static final String FIND_ALL = "select * from t_user limit ?, ?";

}
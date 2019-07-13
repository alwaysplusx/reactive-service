### 编程模型

Vert.x
处理不好有可能造成callback hell, 可以使用future来实现类似的pub/sub功能.

projectreactor有puh/sub模型, 一定程度上减小了callback的使用

下面为给定需求实现的区别

#### 按Id查询数据:

vert.x
```java
Future<List<JsonObject>> future = Future.future();
SQLClient client = JDBCClient.create(vertx, dataSource);
client.getConnection(ar -> {
    SQLConnection conn = ar.result();
    conn.queryWithParams(FIND_BY_ID, params, result -> {
        future.complete(result.result().getRows());
        conn.close();
    });
});
future.setHandler(ar -> {
    log.info(ar.result().toString());// ar.result() = List<JsonObject>
});
```

projectreactor
```java
// r2dbc connection factory
Publisher<Connection> pub = connectionFactory.create();
Mono user = Mono.from(pub)
                .flatMap(conn -> Mono.from(conn.createStatement(FIND_BY_ID).bind(0, id).execute()))
                .flatMap(result -> Mono.from(MAPPING_FUNCTION));
```

### 并发测试
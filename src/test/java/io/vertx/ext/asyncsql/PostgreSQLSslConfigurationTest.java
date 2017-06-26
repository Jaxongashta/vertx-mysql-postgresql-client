package io.vertx.ext.asyncsql;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

import java.util.List;

/**
 * Tests the configuration options of the PostgreSQL client.
 */
public class PostgreSQLSslConfigurationTest extends ConfigurationTest {

  @Override
  protected SQLClient createClient(Vertx vertx, JsonObject config) {
    return PostgreSQLClient.createNonShared(vertx, config);
  }

  @Override
  public String sleepCommand(int seconds) {
    return "pg_sleep(" + seconds + ")";
  }

  @Override
  protected String getEncodingStatement() {
    return "SHOW client_encoding";
  }

  @Override
  protected String getEncodingValueFromResults(List<JsonArray> results) {
    return results.get(0).getString(0);
  }

  @Test
  public void testCorrectSslConfiguration(TestContext context) {
    Async async = context.async();
    String path = getClass()
      .getResource("/ssl-docker/server.crt")
      .getPath();

    System.out.println("Path = " + path);

    JsonObject sslConfig = new JsonObject()
      .put("sslMode", "require")
      .put("sslRootCert", path);

    client = createClient(vertx, sslConfig);

    System.out.println("testCorrectSslConfiguration");
    client.getConnection(sqlConnectionAsyncResult -> {
      System.out.println("testCorrectSslConfiguration callback");
      sqlConnectionAsyncResult.cause().printStackTrace();
      context.assertTrue(sqlConnectionAsyncResult.succeeded());
      conn = sqlConnectionAsyncResult.result();
      System.out.println("testCorrectSslConfiguration step2");
      conn.query("SELECT 1", ar -> {
        System.out.println("testCorrectSslConfiguration callback2");
        if (ar.failed()) {
          context.fail("Should not fail on ssl connection");
        } else {
          System.out.println("testCorrectSslConfiguration all good!");
          async.complete();
        }
      });
    });
  }

  @Test
  public void testWrongSslConfiguration(TestContext context) {
    Async async = context.async();
    client = createClient(vertx,
      new JsonObject()
        .put("host", System.getProperty("db.host", "localhost"))
        .put("sslMode", "verify-ca")
        .put("sslRootCert", "something-wrong.crt")
    );

    System.out.println("testWrongSslConfiguration");
    client.getConnection(sqlConnectionAsyncResult -> {
      System.out.println("testWrongSslConfiguration callback");
      context.assertTrue(sqlConnectionAsyncResult.failed());
      System.out.println("testWrongSslConfiguration success!");
      async.complete();
    });
  }

}
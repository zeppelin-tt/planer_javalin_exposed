package planer

import io.javalin.Javalin
import io.javalin.Javalin.log
import io.javalin.core.JavalinConfig
import io.javalin.plugin.json.JavalinJackson
import io.javalin.plugin.openapi.annotations.ContentType
import org.jetbrains.exposed.sql.Database
import planer.dao.NoteDao
import planer.handler.CommandSocketHandler
import planer.util.connectCheck
import planer.util.getProp
import planer.util.globalObjectMapper

fun main() {
    // получение данных для инициализации БД
    val url = getProp("DB_URL", "db.url", "jdbc:postgresql://localhost:5432/planer")
    val driver = getProp("db.driver", "org.postgresql.Driver")
    val user = getProp("DB_USER", "db.user", "postgres")
    val password = getProp("DB_PASSWORD", "db.password", "superpass")

    // ожидание запуска БД
    (getProp("db.attemptConnectionCount", "10")).toInt()
        .also { connectCheck(it, url, user, password) }

    // соединение с БД
    Database.connect(url, driver, user, password)

    log.info("url: [$url]")
    log.info("user: [$user]")
    log.info("password: [$password]")

    // создание схем и таблиц в БД
    NoteDao().init()

    // запуск сервера Javalin
    val connect = startServer()

    // добавлен handler c точкой входа
    CommandSocketHandler().addHandler(connect)
}

fun startServer(): Javalin = Javalin
    .create { initialConfig(it) }
    .start(8000)

fun initialConfig(config: JavalinConfig) = config.apply {
    wsFactoryConfig { wsFactory ->
        wsFactory.policy.maxTextMessageSize = 10_000
        wsFactory.policy.idleTimeout = 100_000_000
    }
    defaultContentType = ContentType.JSON
    JavalinJackson.configure(globalObjectMapper)
    enableDevLogging()
}

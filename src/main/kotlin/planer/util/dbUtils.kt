package planer.util

import io.javalin.Javalin.log
import java.sql.DriverManager
import java.util.*


fun connectCheck(count: Int, url: String, user: String, password: String) {
    val props = Properties().apply {
        setProperty("user", user)
        setProperty("password", password)
    }
    log.info("trying connect to db...")
    for (i in 0 until count) {
        try {
            DriverManager.getConnection(url, props)
        } catch (e: Exception) {
            if (i < 10) {
                Thread.sleep(1000)
                continue
            } else {
                log.error("connection not complete over $count seconds")
                throw e
            }
        }
        log.info("connection complete over $i seconds")
        return
    }

}

package com.smena.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val dataSource = createHikariDataSource(config)
        runMigrations(dataSource)
        Database.connect(dataSource)
    }

    private fun createHikariDataSource(config: ApplicationConfig): HikariDataSource {
        val dbConfig = config.config("database")
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbConfig.property("jdbcUrl").getString()
            username = dbConfig.property("username").getString()
            password = dbConfig.property("password").getString()
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = dbConfig.propertyOrNull("poolSize")?.getString()?.toInt() ?: 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }

    private fun runMigrations(dataSource: HikariDataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}

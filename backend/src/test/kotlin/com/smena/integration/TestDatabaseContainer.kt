package com.smena.integration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer

object TestDatabaseContainer {
    private val container: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
        .withDatabaseName("smena_test")
        .withUsername("test")
        .withPassword("test")

    private var initialized = false
    private lateinit var dataSource: HikariDataSource

    fun start() {
        if (!initialized) {
            container.start()
            dataSource = createDataSource()
            runMigrations()
            Database.connect(dataSource)
            initialized = true
        }
    }

    fun getJdbcUrl(): String = container.jdbcUrl
    fun getUsername(): String = container.username
    fun getPassword(): String = container.password
    fun getConnection() = dataSource.connection

    fun cleanDatabase() {
        if (initialized) {
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("""
                        TRUNCATE TABLE lineups, registrations, events, team_members, teams, users RESTART IDENTITY CASCADE
                    """.trimIndent())
                }
            }
        }
    }

    private fun createDataSource(): HikariDataSource {
        val config = HikariConfig().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }

    private fun runMigrations() {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}

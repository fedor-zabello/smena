# Hockey App — Backend

Kotlin + Ktor backend для Telegram Mini App.

## Технологии

- **Kotlin** 1.9+
- **Ktor** 2.x — веб-фреймворк
- **Exposed** — ORM для PostgreSQL
- **kotlinx.serialization** — JSON сериализация
- **HikariCP** — connection pool

## Структура

```
src/main/kotlin/com/hockeyapp/
├── Application.kt          # Точка входа
├── plugins/                # Конфигурация Ktor
├── routes/                 # HTTP эндпоинты
├── services/               # Бизнес-логика
├── repositories/           # Работа с БД
├── models/                 # Domain модели
└── dto/                    # Request/Response
```

## Запуск

```bash
# Разработка
./gradlew run

# Тесты
./gradlew test

# Сборка JAR
./gradlew shadowJar
```

## Конфигурация

Переменные окружения (или `application.conf`):

```
DATABASE_URL=jdbc:postgresql://localhost:5432/hockey_app
DATABASE_USER=postgres
DATABASE_PASSWORD=secret
TELEGRAM_BOT_TOKEN=your-bot-token
```

## API

См. `docs/plan/T-001.md` для описания эндпоинтов.

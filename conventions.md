# Технические соглашения (Conventions)

Этот документ описывает правила и соглашения для кодовой базы проекта Hockey Team App.

## Общие правила

### Язык
- Код: английский (названия переменных, функций, классов)
- Комментарии: русский
- Документация: русский
- Git коммиты: русский или английский

### Git
- Формат коммита: `[T-XXX] Краткое описание`
- Одна задача = один коммит (или несколько логически связанных)
- Не коммитить: секреты, .env файлы, node_modules, build артефакты

---

## Backend (Kotlin + Ktor)

### Структура пакетов

```
com.hockeyapp/
├── Application.kt           # Точка входа, конфигурация Ktor
├── plugins/                 # Ktor plugins
│   ├── Authentication.kt    # Telegram auth
│   ├── Cors.kt
│   ├── Serialization.kt
│   └── Database.kt          # Подключение к PostgreSQL
├── routes/                  # HTTP endpoints
│   ├── EventRoutes.kt
│   ├── TeamRoutes.kt
│   └── UserRoutes.kt
├── services/                # Бизнес-логика
│   ├── EventService.kt
│   ├── TeamService.kt
│   └── UserService.kt
├── repositories/            # Работа с БД через Exposed
│   ├── EventRepository.kt
│   ├── TeamRepository.kt
│   └── UserRepository.kt
├── models/                  # Domain модели
│   ├── User.kt
│   ├── Team.kt
│   └── Event.kt
├── dto/                     # Request/Response объекты
│   ├── EventDto.kt
│   └── UserDto.kt
└── utils/                   # Утилиты
    └── TelegramAuth.kt
```

### Именование (Kotlin)

| Что | Стиль | Пример |
|-----|-------|--------|
| Классы, объекты | PascalCase | `EventService`, `UserRepository` |
| Функции, переменные | camelCase | `getEventById`, `currentUser` |
| Константы | SCREAMING_SNAKE_CASE | `MAX_PLAYERS_COUNT` |
| Пакеты | lowercase | `com.hockeyapp.services` |
| Таблицы БД (Exposed) | PascalCase + "Table" | `UsersTable`, `EventsTable` |

### Правила Ktor

```kotlin
// Роуты группируем по сущностям
fun Application.configureEventRoutes() {
    routing {
        route("/api/events") {
            get { /* список событий */ }
            post { /* создать событие */ }
            route("/{id}") {
                get { /* одно событие */ }
                put { /* обновить */ }
                delete { /* удалить */ }
            }
        }
    }
}

// Используем extension functions для чистоты
fun Route.eventRoutes(eventService: EventService) {
    get {
        val events = eventService.getAll()
        call.respond(events)
    }
}
```

### Правила Exposed (ORM)

```kotlin
// Таблицы описываем как object
object UsersTable : LongIdTable("users") {
    val telegramId = long("telegram_id").uniqueIndex()
    val name = varchar("name", 255)
    val role = enumerationByName<UserRole>("role", 20)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

// Entity классы
class UserEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserEntity>(UsersTable)
    
    var telegramId by UsersTable.telegramId
    var name by UsersTable.name
    var role by UsersTable.role
}

// Транзакции оборачиваем явно
suspend fun createUser(dto: CreateUserDto): User = dbQuery {
    UserEntity.new {
        telegramId = dto.telegramId
        name = dto.name
        role = UserRole.PLAYER
    }.toModel()
}
```

### Обработка ошибок

```kotlin
// Кастомные исключения
class NotFoundException(message: String) : Exception(message)
class ForbiddenException(message: String) : Exception(message)
class ValidationException(message: String) : Exception(message)

// Глобальный обработчик в Ktor
install(StatusPages) {
    exception<NotFoundException> { call, cause ->
        call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message))
    }
    exception<ForbiddenException> { call, cause ->
        call.respond(HttpStatusCode.Forbidden, ErrorResponse(cause.message))
    }
}
```

### Зависимости Backend (разрешённые)

```kotlin
// build.gradle.kts
dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-auth")
    
    // Database
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-dao")
    implementation("org.jetbrains.exposed:exposed-jdbc")
    implementation("org.jetbrains.exposed:exposed-java-time")
    implementation("org.postgresql:postgresql")
    implementation("com.zaxxer:HikariCP")
    
    // Utils
    implementation("ch.qos.logback:logback-classic")
    
    // Testing
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
```

---

## Frontend (React + TypeScript)

### Структура папок

```
src/
├── main.tsx                 # Точка входа
├── App.tsx                  # Корневой компонент + роутинг
├── components/              # Переиспользуемые компоненты
│   ├── ui/                  # Базовые UI элементы
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   └── Input.tsx
│   ├── EventCard.tsx
│   └── PlayerList.tsx
├── pages/                   # Страницы (роуты)
│   ├── HomePage.tsx
│   ├── EventPage.tsx
│   └── TeamPage.tsx
├── hooks/                   # Кастомные хуки
│   ├── useEvents.ts
│   └── useTelegram.ts
├── api/                     # API клиент
│   ├── client.ts            # Базовый fetch wrapper
│   ├── events.ts
│   └── teams.ts
├── types/                   # TypeScript типы
│   ├── event.ts
│   ├── user.ts
│   └── team.ts
├── utils/                   # Утилиты
│   └── formatDate.ts
└── styles/                  # Глобальные стили (если нужны)
    └── index.css
```

### Именование (TypeScript/React)

| Что | Стиль | Пример |
|-----|-------|--------|
| Компоненты | PascalCase | `EventCard.tsx`, `PlayerList.tsx` |
| Хуки | camelCase с "use" | `useEvents.ts`, `useTelegram.ts` |
| Утилиты, API | camelCase | `formatDate.ts`, `client.ts` |
| Типы/Интерфейсы | PascalCase | `Event`, `User`, `CreateEventRequest` |
| Переменные, функции | camelCase | `eventList`, `handleSubmit` |
| Константы | SCREAMING_SNAKE_CASE | `API_BASE_URL` |

### Правила React

```tsx
// Функциональные компоненты с явной типизацией
interface EventCardProps {
  event: Event;
  onRegister: (eventId: string) => void;
}

export function EventCard({ event, onRegister }: EventCardProps) {
  // Хуки в начале
  const [isLoading, setIsLoading] = useState(false);
  
  // Обработчики
  const handleClick = () => {
    setIsLoading(true);
    onRegister(event.id);
  };
  
  // JSX
  return (
    <div className="event-card">
      <h3>{event.title}</h3>
      <button onClick={handleClick} disabled={isLoading}>
        {isLoading ? 'Загрузка...' : 'Записаться'}
      </button>
    </div>
  );
}
```

### Правила API клиента

```typescript
// api/client.ts
const API_BASE_URL = import.meta.env.VITE_API_URL;

export async function apiRequest<T>(
  endpoint: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      // Telegram auth header
      'X-Telegram-Init-Data': window.Telegram?.WebApp?.initData ?? '',
      ...options?.headers,
    },
  });
  
  if (!response.ok) {
    throw new Error(`API Error: ${response.status}`);
  }
  
  return response.json();
}

// api/events.ts
export const eventsApi = {
  getAll: () => apiRequest<Event[]>('/api/events'),
  getById: (id: string) => apiRequest<Event>(`/api/events/${id}`),
  register: (id: string) => apiRequest<void>(`/api/events/${id}/register`, { method: 'POST' }),
};
```

### Telegram Mini App интеграция

```tsx
// hooks/useTelegram.ts
import { useEffect } from 'react';

export function useTelegram() {
  const tg = window.Telegram?.WebApp;
  
  useEffect(() => {
    tg?.ready();
    tg?.expand();
  }, []);
  
  return {
    user: tg?.initDataUnsafe?.user,
    initData: tg?.initData,
    colorScheme: tg?.colorScheme,
    showAlert: (message: string) => tg?.showAlert(message),
    close: () => tg?.close(),
  };
}
```

### Зависимости Frontend (разрешённые)

```json
{
  "dependencies": {
    "react": "^18.x",
    "react-dom": "^18.x",
    "react-router-dom": "^6.x",
    "@telegram-apps/sdk-react": "latest"
  },
  "devDependencies": {
    "typescript": "^5.x",
    "vite": "^5.x",
    "@types/react": "^18.x",
    "@types/react-dom": "^18.x",
    "tailwindcss": "^3.x",
    "autoprefixer": "latest",
    "postcss": "latest"
  }
}
```

---

## База данных (PostgreSQL)

### Схема таблиц

```sql
-- Пользователи
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Команды
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    invite_code VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Связь пользователей и команд
CREATE TABLE team_members (
    user_id BIGINT REFERENCES users(id),
    team_id BIGINT REFERENCES teams(id),
    role VARCHAR(20) NOT NULL DEFAULT 'PLAYER', -- PLAYER, COACH
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, team_id)
);

-- События (игры, тренировки)
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT REFERENCES teams(id),
    title VARCHAR(255) NOT NULL,
    event_type VARCHAR(20) NOT NULL, -- GAME, TRAINING
    event_date TIMESTAMP NOT NULL,
    location VARCHAR(500),
    max_players INT,
    status VARCHAR(20) DEFAULT 'OPEN', -- OPEN, CLOSED, CANCELLED
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Записи на события
CREATE TABLE registrations (
    user_id BIGINT REFERENCES users(id),
    event_id BIGINT REFERENCES events(id),
    status VARCHAR(20) NOT NULL DEFAULT 'GOING', -- GOING, MAYBE, NOT_GOING
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, event_id)
);

-- Состав на событие (формирует тренер)
CREATE TABLE lineup (
    event_id BIGINT REFERENCES events(id),
    user_id BIGINT REFERENCES users(id),
    position VARCHAR(20) NOT NULL, -- PLAYING, RESERVE, BENCH
    PRIMARY KEY (event_id, user_id)
);
```

### Правила миграций

- Миграции храним в `backend/src/main/resources/db/migration/`
- Формат имени: `V001__create_users_table.sql`
- Используем Flyway или ручные скрипты
- Никогда не меняем уже применённые миграции

---

## Тестирование

### Backend
- Unit тесты для сервисов
- Integration тесты для репозиториев (с testcontainers или H2)
- API тесты через Ktor test host

### Frontend
- Компоненты: React Testing Library
- Хуки: @testing-library/react-hooks
- E2E: опционально (Playwright)

---

## Безопасность

### Секреты
- Никогда не коммитить в репозиторий
- Хранить в `.env` файлах (локально) и environment variables (продакшн)
- `.env` добавлен в `.gitignore`

### Telegram аутентификация
- Всегда валидировать `initData` на бэкенде
- Проверять подпись через Bot Token
- Не доверять данным с фронтенда без валидации

### API
- CORS настроен только для домена Mini App
- Rate limiting для защиты от спама
- Валидация всех входных данных

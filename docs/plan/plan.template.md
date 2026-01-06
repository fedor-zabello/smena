# План: $TICKET

**Status:** DRAFT | PLAN_APPROVED

**PRD:** `docs/prd/$TICKET.prd.md`

## Обзор решения

Краткое описание архитектурного подхода (2-3 предложения).

## Компоненты

### Backend

Какие модули/сервисы будут затронуты или созданы?

```
backend/src/main/kotlin/
├── routes/
│   └── [NewRoutes.kt]       # Новые эндпоинты
├── services/
│   └── [NewService.kt]      # Новая бизнес-логика
├── repositories/
│   └── [NewRepository.kt]   # Работа с БД
├── models/
│   └── [NewModel.kt]        # Новые модели
└── dto/
    └── [NewDto.kt]          # Request/Response
```

### Frontend

Какие компоненты/страницы будут затронуты или созданы?

```
frontend/src/
├── pages/
│   └── [NewPage.tsx]        # Новая страница
├── components/
│   └── [NewComponent.tsx]   # Новый компонент
├── hooks/
│   └── [useNew.ts]          # Новый хук
└── api/
    └── [new.ts]             # API методы
```

### База данных

Новые таблицы или изменения в существующих:

```sql
-- Новая таблица (если нужна)
CREATE TABLE new_table (
    id BIGSERIAL PRIMARY KEY,
    ...
);

-- Изменения существующей (если нужны)
ALTER TABLE existing_table ADD COLUMN new_column TYPE;
```

## API Контракт

### Новые эндпоинты

```
GET    /api/resource           # Список
POST   /api/resource           # Создать
GET    /api/resource/{id}      # Получить один
PUT    /api/resource/{id}      # Обновить
DELETE /api/resource/{id}      # Удалить
```

### Примеры запросов/ответов

**GET /api/resource**
```json
// Response 200
{
  "items": [
    { "id": 1, "name": "..." }
  ]
}
```

**POST /api/resource**
```json
// Request
{ "name": "..." }

// Response 201
{ "id": 1, "name": "..." }
```

## Потоки данных

Описание основных сценариев:

### Сценарий 1: [Название]

```
User → Frontend → POST /api/xxx → Backend → Database
                                     ↓
User ← Frontend ← Response 200 ←────┘
```

## NFR (Non-Functional Requirements)

- **Производительность:** ожидаемая нагрузка, время ответа
- **Безопасность:** авторизация, валидация
- **Масштабируемость:** что если пользователей станет больше

## Риски и альтернативы

### Риск 1: [Описание]

**Решение:** ...

### Альтернативный подход (отклонён)

Почему не выбрали другой путь.

## Зависимости

- Что должно быть готово до начала работы
- Внешние сервисы/библиотеки

## Открытые технические вопросы

- [ ] Вопрос 1?
- [ ] Вопрос 2?

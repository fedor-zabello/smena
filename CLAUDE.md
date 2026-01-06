# Hockey Team App — Инструкции для Claude Code

## О проекте

Telegram Mini App для любительских хоккейных команд.

**MVP функциональность:**
- Игроки записываются на игры и тренировки
- Администратор создаёт события
- Тренер формирует состав
- Уведомления через Telegram

## Технический стек

### Backend
- **Язык:** Kotlin 2.3+
- **Фреймворк:** Ktor 3.x
- **ORM:** Exposed 0.58+
- **База данных:** PostgreSQL
- **Сериализация:** kotlinx.serialization
- **Сборка:** Gradle (Kotlin DSL)

### Frontend
- **Язык:** TypeScript 5.9+
- **Фреймворк:** React 19+
- **Сборка:** Vite 7.x
- **Telegram SDK:** @telegram-apps/sdk-react
- **Стили:** Tailwind CSS 4.x
- **HTTP клиент:** fetch / axios

### Инфраструктура
- **Хостинг:** VPS (systemctl для backend, nginx для frontend)
- **SSL:** Let's Encrypt
- **База:** PostgreSQL на том же VPS

## Структура проекта

```
smena/
├── backend/                 # Kotlin + Ktor
│   ├── src/main/kotlin/
│   │   ├── Application.kt   # Точка входа
│   │   ├── plugins/         # Ktor plugins (Auth, CORS, etc.)
│   │   ├── routes/          # HTTP endpoints
│   │   ├── services/        # Бизнес-логика
│   │   ├── repositories/    # Работа с БД
│   │   ├── models/          # Domain модели
│   │   └── dto/             # Data Transfer Objects
│   ├── src/main/resources/
│   │   └── application.conf # Конфигурация Ktor
│   └── build.gradle.kts
│
├── frontend/                # React + TypeScript
│   ├── src/
│   │   ├── main.tsx         # Точка входа
│   │   ├── App.tsx          # Корневой компонент
│   │   ├── components/      # UI компоненты
│   │   ├── pages/           # Страницы
│   │   ├── hooks/           # React хуки
│   │   ├── api/             # API клиент
│   │   ├── types/           # TypeScript типы
│   │   └── utils/           # Утилиты
│   ├── index.html
│   └── package.json
│
├── shared/                  # Общие типы (опционально)
├── docs/                    # AIDD артефакты
└── CLAUDE.md               # Этот файл
```

## Workflow: как работать с этим проектом

### Перед любыми правками кода

1. **Проверь активный тикет** в `docs/.active_ticket`
2. **Прочитай артефакты тикета:**
   - `docs/prd/<ticket>.prd.md` — что делаем и зачем
   - `docs/plan/<ticket>.md` — архитектура решения
   - `docs/tasklist/<ticket>.md` — список задач
3. **Найди текущую задачу** — первый незакрытый `- [ ]` в tasklist
4. **Сформулируй план изменений** и покажи его мне
5. **Дождись моего "ОК"** перед началом кодинга

### Во время работы

1. **Делай минимальные изменения** — только то, что нужно для текущей задачи
2. **Пиши тесты** для бизнес-логики
3. **Следуй conventions.md** — там правила кодстайла

### После правок

1. **Покажи diff** — что изменилось
2. **Предложи текст коммита** в формате: `[T-XXX] Краткое описание`
3. **Обнови tasklist** — поставь `- [x]` у выполненной задачи
4. **Остановись** и дождись моего ревью

### Запрещено без явного разрешения

- Переходить к следующей задаче без подтверждения
- Менять архитектуру, не описанную в плане
- Добавлять зависимости, не указанные в conventions.md
- Удалять или существенно менять существующий код вне scope задачи

## Версии зависимостей

Перед добавлением зависимостей в build.gradle.kts или package.json — 
проверь актуальные версии через веб-поиск или официальную документацию.

## Команды для работы

При работе в Claude Code используй эти паттерны:

```
# Посмотреть текущий тикет
cat docs/.active_ticket

# Прочитать PRD
cat docs/prd/T-001.prd.md

# Прочитать план
cat docs/plan/T-001.md

# Посмотреть задачи
cat docs/tasklist/T-001.md

# Backend: запуск
cd backend && ./gradlew run

# Backend: тесты
cd backend && ./gradlew test

# Frontend: запуск
cd frontend && npm run dev

# Frontend: тесты
cd frontend && npm test
```

## Контекст для AI

Когда я прошу что-то сделать без указания тикета — сначала спроси, к какому тикету это относится, или предложи создать новый.

Если задача кажется слишком большой для одного шага — предложи разбить её на подзадачи.

Если что-то непонятно в PRD или плане — спроси, не додумывай.

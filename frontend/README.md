# Hockey App — Frontend

React + TypeScript frontend для Telegram Mini App.

## Технологии

- **React** 18+
- **TypeScript** 5.x
- **Vite** — сборка
- **@telegram-apps/sdk-react** — Telegram Mini App SDK
- **Tailwind CSS** — стили

## Структура

```
src/
├── main.tsx               # Точка входа
├── App.tsx                # Корневой компонент
├── pages/                 # Страницы
├── components/            # UI компоненты
├── hooks/                 # React хуки
├── api/                   # API клиент
├── types/                 # TypeScript типы
└── utils/                 # Утилиты
```

## Запуск

```bash
# Установка зависимостей
npm install

# Разработка
npm run dev

# Сборка
npm run build

# Превью продакшн сборки
npm run preview
```

## Конфигурация

Переменные окружения (`.env`):

```
VITE_API_URL=https://your-api.domain.com
VITE_BOT_USERNAME=your_bot_username
```

## Telegram Mini App

Для локальной разработки:

1. Используй ngrok для HTTPS туннеля
2. Настрой BotFather: `/setmenubutton` с URL ngrok
3. Открой бота в Telegram и нажми кнопку меню

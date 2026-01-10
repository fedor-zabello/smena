-- Пользователи (из Telegram)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255),
    username VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Команды
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    invite_code VARCHAR(8) UNIQUE NOT NULL,
    telegram_chat_id BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Членство в команде
CREATE TABLE team_members (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    team_id BIGINT REFERENCES teams(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('PLAYER', 'COACH', 'ADMIN')),
    joined_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, team_id)
);

-- События
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT REFERENCES teams(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('GAME', 'TRAINING')),
    title VARCHAR(255),
    description TEXT,
    event_date DATE NOT NULL,
    event_time TIME NOT NULL,
    location VARCHAR(255),
    max_players INT,
    registration_opens_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED', 'OPEN', 'CLOSED', 'CANCELLED')),
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Записи на события
CREATE TABLE registrations (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT REFERENCES events(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('GOING', 'NOT_GOING')),
    registered_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(event_id, user_id)
);

-- Состав на игру/тренировку
CREATE TABLE lineups (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT REFERENCES events(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    line_group VARCHAR(20) NOT NULL,
    position VARCHAR(10),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(event_id, user_id)
);

-- Индексы
CREATE INDEX idx_events_team_date ON events(team_id, event_date);
CREATE INDEX idx_events_registration_opens ON events(registration_opens_at) WHERE status = 'SCHEDULED';
CREATE INDEX idx_registrations_event ON registrations(event_id);
CREATE INDEX idx_lineups_event ON lineups(event_id);
CREATE INDEX idx_team_members_user ON team_members(user_id);

# Telegram Reminder Bot

Этот бот позволяет создавать и управлять напоминаниями через Telegram. Он поддерживает различные типы напоминаний: ежедневные, еженедельные, ежемесячные и одноразовые.

## Функциональность

- Создание напоминаний с указанием имени, времени и частоты
- Просмотр списка ваших напоминаний
- Удаление напоминаний
- Редактирование существующих напоминаний
- Автоматические уведомления в указанное время
- Удобный интерфейс с кнопками

## Команды бота

- `/start` - Начать работу с ботом
- `/help` - Показать список доступных команд
- `/createevent` - Создать новое напоминание
- `/listevents` - Показать список ваших напоминаний
- `/deleteevent` - Удалить напоминание
- `/editevent` - Изменить существующее напоминание

## Установка и запуск

1. Клонируйте репозиторий:
```bash
git clone <repository-url>
cd telegram-reminder-bot
```

2. Создайте бота в Telegram через @BotFather и получите токен

3. Настройте конфигурацию в `src/main/resources/application.properties`:
```properties
telegram.bot.username=YOUR_BOT_USERNAME
telegram.bot.token=YOUR_BOT_TOKEN
```

4. Соберите проект:
```bash
mvn clean package
```

5. Запустите приложение:
```bash
java -jar target/telegram-reminder-bot-0.0.1-SNAPSHOT.jar
```

## Технологии

- Java 11
- Spring Boot
- Spring Data JPA
- SQLite
- Telegram Bot API

## Развертывание

Приложение можно развернуть на любом хостинге, поддерживающем Java. Рекомендуемые бесплатные варианты:

1. Heroku
2. Railway
3. Render

## Лицензия

MIT 
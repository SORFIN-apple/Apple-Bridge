# AppleBridge Paper

`AppleBridge Paper` — это плагин для серверов Minecraft на `Paper`, который позволяет выполнять команды сервера через HTTP API.

Проще говоря:

- Minecraft-сервер запускает маленький HTTP API
- ваш Discord-бот отправляет туда запрос
- сервер получает команду и выполняет её от имени консоли

Плагин **не содержит Discord-код**. Он нужен как мост между Minecraft и вашим будущим Discord-ботом.

## Для чего это нужно

С помощью этого плагина можно сделать, например:

- Discord-команду для отправки сообщения в Minecraft
- Discord-команду для выдачи предметов
- Discord-команду для кика, бана или whitelist
- удалённое выполнение админ-команд через собственного бота

## Что умеет плагин

- запускает встроенный HTTP API внутри сервера
- принимает `POST` запросы на `/execute`
- проверяет секретный токен через заголовок `Authorization`
- выполняет команду через консоль Minecraft
- позволяет перезагрузить конфиг без полного рестарта сервера
- показывает статус API через команду

## Совместимость

Один и тот же `.jar` подходит для:

- `Paper 1.19.x`
- `Paper 1.20.x`
- `Paper 1.21.x`

Важно по Java:

- `Paper 1.19.x` обычно требует `Java 17`
- `Paper 1.20.x` и `Paper 1.21.x` обычно требуют `Java 21`

Если у вас не подходит версия Java, проблема будет не в плагине, а в самом запуске сервера.

## Установка

### 1. Убедитесь, что у вас именно Paper

Этот плагин рассчитан на `Paper`.

Если у вас:

- `Paper` → подходит
- `Purpur` → обычно тоже подойдёт, потому что он совместим с Paper
- `Spigot` или `Bukkit` → официально не целимся в них

### 2. Скопируйте `.jar` в папку plugins

Поместите файл плагина в папку:

```text
plugins/
```

Пример:

```text
plugins/AppleBridge-Paper-1.0.0.jar
```

### 3. Запустите сервер

После первого запуска Paper создаст папку плагина и конфиг.

Обычно появится путь:

```text
plugins/AppleBridge/config.yml
```

### 4. Откройте config.yml

Стандартный конфиг выглядит так:

```yml
enabled: true
port: 8080
secret: "CHANGE_ME"
```

Теперь разберём каждую настройку отдельно.

## Настройка config.yml

### `enabled`

```yml
enabled: true
```

Включает или выключает HTTP API.

- `true` → API работает
- `false` → API отключён

Если хотите временно выключить доступ для бота, можно поставить:

```yml
enabled: false
```

После этого выполните:

```text
/applebridge reload
```

### `port`

```yml
port: 8080
```

Это порт, на котором будет работать API.

Пример адреса API:

```text
http://127.0.0.1:8080/execute
```

Что важно знать:

- порт не должен быть занят другой программой
- если бот работает на другой машине, этот порт может понадобиться открыть в firewall
- если бот и Minecraft работают на одной машине, порт можно не открывать наружу

Если `8080` занят, можете сменить, например, на:

```yml
port: 8123
```

Тогда адрес API станет:

```text
http://127.0.0.1:8123/execute
```

### `secret`

```yml
secret: "CHANGE_ME"
```

Это главный защитный токен.

Без правильного `secret` запросы будут отклоняться.

Рекомендуется заменить его на длинную случайную строку. Например:

```yml
secret: "4NfJz9LxA1vQp8YeK2tRm7DsC0uBw3Hg"
```

Если оставить `CHANGE_ME`, плагин сам сгенерирует случайный секрет при запуске и сохранит его в конфиг.

Очень важно:

- никому не показывайте `secret`
- не публикуйте его на GitHub
- не скидывайте его в Discord
- не оставляйте его на скриншотах

Если кто-то знает ваш `secret` и может достучаться до API-порта, он сможет выполнять команды сервера.

## Как сохранить изменения

После изменения `config.yml` у вас есть 2 варианта:

### Вариант 1. Перезапустить весь сервер

Просто полностью перезапустите Paper.

### Вариант 2. Перезагрузить только конфиг плагина

Выполните:

```text
/applebridge reload
```

Это перечитает `config.yml` и перезапустит HTTP API с новыми настройками.

## Команды плагина

### `/applebridge reload`

Перезагружает `config.yml`.

Используйте после изменения:

- `enabled`
- `port`
- `secret`

### `/applebridge status`

Показывает состояние API.

Вы увидите:

- включён ли API
- запущен ли HTTP сервер
- какой порт используется

Это удобно для быстрой проверки после настройки.

## Как понять, что плагин работает

При успешном запуске в консоли сервера появится строка:

```text
AppleBridge API started on port X
```

Например:

```text
AppleBridge API started on port 8080
```

Также можно использовать:

```text
/applebridge status
```

Если `running: true`, значит API запущен.

## Как работает API

Плагин слушает endpoint:

```text
POST /execute
```

Тело запроса должно быть таким:

```json
{
  "command": "say hello"
}
```

Также должен быть header:

```text
Authorization: ВАШ_SECRET
```

Если всё верно:

- Minecraft выполнит команду из консоли
- API вернёт:

```text
OK
```

Если `secret` неправильный:

- API вернёт `403 Forbidden`

Если JSON сломан или команда пустая:

- API вернёт `400 Invalid JSON`

## Просмотр последних логов консоли

Плагин также умеет отдавать последние строки лога сервера через отдельный endpoint:

```text
GET /logs
```

Этот endpoint защищён тем же `Authorization` header, что и `/execute`.

### Пример запроса

```text
GET /logs?limit=20
```

Параметр `limit` необязательный.

- если не указать, плагин вернёт последние `50` строк
- максимум можно запросить `200` строк за раз

### Пример через PowerShell

```powershell
Invoke-RestMethod -Uri 'http://IP_ИЛИ_ДОМЕН:PORT/logs?limit=20' `
  -Method GET `
  -Headers @{ Authorization = 'ВАШ_SECRET' }
```

### Пример через curl

```bash
curl -H "Authorization: ВАШ_SECRET" "http://IP_ИЛИ_ДОМЕН:PORT/logs?limit=20"
```

### Что возвращает `/logs`

Плагин возвращает обычный текст, по одной строке на запись.

Пример:

```text
[23:04:10] [INFO] [Minecraft] Done (12.345s)! For help, type "help"
[23:05:22] [INFO] [Minecraft] Player123 joined the game
[23:06:16] [INFO] [Minecraft] [Server] hello from api
```

## Новая настройка в config.yml

### `log-buffer-size`

```yml
log-buffer-size: 200
```

Это размер внутреннего буфера последних строк лога.

Как это работает:

- плагин хранит не весь лог-файл целиком
- он запоминает только последние строки в памяти
- когда приходят новые записи, старые постепенно вытесняются

Примеры:

- `50` → хранить последние 50 строк
- `200` → хранить последние 200 строк
- `500` → хранить последние 500 строк

Для большинства случаев `200` более чем достаточно.

## Самый простой способ настройки

Ниже 2 самых частых сценария.

## Сценарий 1. Бот и Minecraft на одной машине

Это **лучший и самый безопасный вариант**.

Пример:

- Minecraft-сервер работает на вашем VPS
- Discord-бот тоже работает на этом же VPS

В таком случае:

1. Установите плагин
2. Оставьте или задайте свой `port`, например `8080`
3. Установите длинный `secret`
4. Выполните `/applebridge reload`
5. В боте используйте URL:

```text
http://127.0.0.1:8080/execute
```

Почему это хороший вариант:

- не нужно открывать API в интернет
- меньше риск, что кто-то найдёт ваш порт
- проще настроить

Если бот на той же машине, почти всегда используйте именно:

```text
127.0.0.1
```

а не публичный IP.

## Сценарий 2. Бот на другой машине

Пример:

- Minecraft-сервер работает на одном VPS
- Discord-бот работает на другом VPS

Тогда бот не сможет использовать `127.0.0.1`, потому что это адрес локальной машины.

Вам понадобится:

1. узнать внешний IP или домен сервера
2. открыть API-порт в firewall
3. убедиться, что хостинг не блокирует этот порт
4. указать в боте реальный адрес сервера

Пример:

```text
http://123.123.123.123:8080/execute
```

или:

```text
http://mc.example.com:8080/execute
```

Этот вариант рабочий, но менее безопасный, потому что API уже доступен по сети.

Если используете этот сценарий, особенно важно:

- задать сложный `secret`
- ограничить доступ firewall по IP, если это возможно
- не давать Discord-команду всем подряд

## Как проверить плагин вручную

До подключения Discord-бота очень полезно проверить всё обычным HTTP-запросом.

## Проверка через curl на Windows

Откройте терминал и выполните:

```bash
curl -X POST http://127.0.0.1:8080/execute ^
  -H "Authorization: ВАШ_SECRET" ^
  -H "Content-Type: application/json" ^
  -d "{\"command\":\"say hello from api\"}"
```

Если всё работает:

- в Minecraft появится сообщение
- в ответ вы получите:

```text
OK
```

## Проверка через curl на Linux/macOS

```bash
curl -X POST http://127.0.0.1:8080/execute \
  -H "Authorization: ВАШ_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"command":"say hello from api"}'
```

## Проверка с другого сервера

Если бот или тестовая машина находится не там же, где Minecraft:

```bash
curl -X POST http://IP_СЕРВЕРА:8080/execute \
  -H "Authorization: ВАШ_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"command":"list"}'
```

## Как подключить Discord-бота

Сам плагин только принимает HTTP-запросы. Значит, Discord-бот должен отправлять их сам.

Боту нужны 2 вещи:

- адрес API
- секретный токен

Обычно это выглядит так:

```env
APPLEBRIDGE_URL=http://127.0.0.1:8080/execute
APPLEBRIDGE_SECRET=ВАШ_СЕКРЕТ
```

Если бот на другой машине:

```env
APPLEBRIDGE_URL=http://IP_ИЛИ_ДОМЕН:8080/execute
APPLEBRIDGE_SECRET=ВАШ_СЕКРЕТ
```

### Что делает бот

Обычная логика такая:

1. человек пишет slash-команду в Discord
2. бот проверяет, можно ли этому человеку использовать команду
3. бот отправляет HTTP POST на Minecraft API
4. Minecraft выполняет команду
5. бот показывает результат

## Пример запроса, который должен отправлять бот

```http
POST /execute HTTP/1.1
Host: 127.0.0.1:8080
Authorization: ВАШ_SECRET
Content-Type: application/json

{"command":"say hello from discord"}
```

## Пример на Node.js

Если ваш бот написан на `Node.js`, запрос может выглядеть так:

```js
const response = await fetch(process.env.APPLEBRIDGE_URL, {
  method: "POST",
  headers: {
    "Authorization": process.env.APPLEBRIDGE_SECRET,
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    command: "say hello from discord"
  })
});

const text = await response.text();
console.log(response.status, text);
```

## Пример с discord.js

Ниже базовый пример slash-команды:

```js
const { SlashCommandBuilder } = require("discord.js");

module.exports = {
  data: new SlashCommandBuilder()
    .setName("mc")
    .setDescription("Выполнить команду на Minecraft сервере")
    .addStringOption(option =>
      option
        .setName("command")
        .setDescription("Команда для отправки")
        .setRequired(true)
    ),

  async execute(interaction) {
    const command = interaction.options.getString("command", true);

    await interaction.deferReply({ ephemeral: true });

    const response = await fetch(process.env.APPLEBRIDGE_URL, {
      method: "POST",
      headers: {
        "Authorization": process.env.APPLEBRIDGE_SECRET,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ command })
    });

    const text = await response.text();

    if (!response.ok) {
      await interaction.editReply(`Ошибка API: ${response.status} ${text}`);
      return;
    }

    await interaction.editReply(`Команда отправлена. Ответ API: ${text}`);
  }
};
```

## Что важно настроить в боте

Очень желательно, чтобы бот:

- разрешал выполнение команд только админам
- проверял роли в Discord
- логировал, кто отправил команду
- не позволял обычным пользователям выполнять произвольные команды

Иначе вы рискуете дать полный доступ к серверу слишком большому числу людей.

## Безопасность

Плагин выполняет команды **от имени консоли сервера**. Это очень мощный доступ.

Поэтому обязательно:

- используйте длинный случайный `secret`
- не публикуйте `secret`
- не открывайте API наружу без необходимости
- если бот и Minecraft на одной машине, используйте `127.0.0.1`
- ограничьте доступ к Discord-командам по ролям

Если есть возможность, дополнительно:

- ограничьте firewall только IP-адресом машины, где работает бот
- используйте отдельный VPS
- не смешивайте тестовый и боевой `secret`

## Частые проблемы

## Плагин не загружается

Проверьте:

- точно ли это `Paper`
- подходит ли версия Java
- нет ли ошибок в консоли сервера

## `/applebridge status` показывает, что API не запущен

Проверьте:

- `enabled: true`
- не занят ли порт
- нет ли ошибки запуска в логах

## Бот получает `403 Forbidden`

Это почти всегда означает проблему с `secret`.

Проверьте:

- совпадает ли `Authorization`
- нет ли пробелов до или после токена
- не был ли изменён `secret` в конфиге

## Бот получает timeout

Проверьте:

- правильный ли IP или домен
- правильный ли порт
- открыт ли порт
- работает ли сервер
- показывает ли `/applebridge status`, что `running: true`

## Команда не выполняется

Проверьте:

- правильная ли это Minecraft-команда
- выполняется ли она вручную из консоли сервера
- не требует ли команда какого-то другого плагина

## Примеры команд

Вот несколько примеров того, что можно отправить:

```json
{"command":"say Hello from Discord"}
```

```json
{"command":"list"}
```

```json
{"command":"whitelist add PlayerName"}
```

```json
{"command":"give PlayerName diamond 1"}
```

## Короткая памятка

Если хотите настроить всё максимально быстро:

1. Закиньте `.jar` в `plugins/`
2. Запустите сервер
3. Откройте `plugins/AppleBridge/config.yml`
4. Поставьте:

```yml
enabled: true
port: 8080
secret: "ВАШ_ДЛИННЫЙ_СЕКРЕТ"
```

5. Выполните:

```text
/applebridge reload
```

6. Проверьте:

```text
/applebridge status
```

7. В боте укажите:

```env
APPLEBRIDGE_URL=http://127.0.0.1:8080/execute
APPLEBRIDGE_SECRET=ВАШ_ДЛИННЫЙ_СЕКРЕТ
```

Если бот на другой машине, замените `127.0.0.1` на IP или домен сервера.

## Для страницы Modrinth

Короткое описание:

```text
Simple Paper plugin that exposes a secure HTTP API for executing Minecraft server commands from external services like Discord bots.
```

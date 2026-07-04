# AppleBridge Paper

`AppleBridge Paper` is a lightweight plugin for Minecraft servers running on `Paper`.  
It exposes a secure HTTP API that allows external tools to execute server commands and read recent logs.

In simple terms:

- your Minecraft server starts a small HTTP API
- your Discord bot or script sends requests to it
- the server executes the command from the console

The plugin does **not** include Discord code.  
It is meant to be the bridge between Minecraft and your external tools.

## What it can do

- execute console commands over HTTP
- return recent server log lines over HTTP
- protect all requests with a secret token
- reload its config without restarting the full server
- report API status with an in-game command

## Use cases

You can use AppleBridge for things like:

- a Discord command that sends messages into Minecraft
- a Discord command that runs admin commands
- private server control panels
- automation scripts
- remote moderation tools

## Compatibility

Available builds:

- `1.16.x-1.18.x`
- `1.19.x-1.21.x`

Tested and working on:

- `1.16.5`
- `1.17.1`
- `1.18.2`
- `1.19.4`
- `1.20.6`
- `1.21.x`

It should also work on `Purpur` for the same version ranges.

## Installation

### 1. Make sure you are using Paper

This plugin is built for `Paper`.

Usually these are fine:

- `Paper`
- `Purpur` (expected to work as well)

`Spigot` may also work in practice, but this project is primarily built and tested for Paper-compatible servers.

### 2. Pick the correct jar file

Use the build that matches your server version:

- `AppleBridge-Paper-1.16.x-1.18.x-1.0.0.jar`
- `AppleBridge-Paper-1.19.x-1.21.x-1.0.0.jar`

### 3. Put the jar into `plugins/`

Example:

```text
plugins/AppleBridge-Paper-1.19.x-1.21.x-1.0.0.jar
```

### 4. Start the server

After the first startup, the plugin will create:

```text
plugins/AppleBridge/config.yml
```

### 5. Configure the plugin

Default config:

```yml
enabled: true
port: 8080
secret: "CHANGE_ME"
log-buffer-size: 200
```

### 6. Linux setup notes

If you are hosting your server on Linux, installation is the same, but the most common paths look like this:

```text
/home/container/plugins/
/home/container/plugins/AppleBridge/config.yml
/home/container/logs/latest.log
```

This usually applies to:

- Ubuntu or Debian VPS servers
- Pterodactyl hosting
- Docker or other container setups

If your bot runs on the same Linux machine as Minecraft, prefer:

```text
http://127.0.0.1:PORT/execute
```

This keeps the API local and avoids exposing it to the internet.

## Notes for Linux users

If your Minecraft server runs on Linux, the plugin works the same way as on Windows.

Typical Linux paths:

```text
/home/container/plugins/
/home/container/plugins/AppleBridge/config.yml
/home/container/logs/latest.log
```

This is common for:

- self-hosted Paper servers on Ubuntu or Debian
- VPS-based servers
- Pterodactyl hosting
- Docker or container-based setups

If your bot or script runs on the same machine as the Minecraft server, the safest setup is usually:

```text
http://127.0.0.1:PORT/execute
```

This avoids exposing the API publicly.

## Configuration

### `enabled`

```yml
enabled: true
```

- `true` = API is enabled
- `false` = API is disabled

### `port`

```yml
port: 8080
```

The HTTP port used by AppleBridge.

Example endpoint:

```text
http://127.0.0.1:8080/execute
```

If your bot runs on another machine, you may need to open this port in your firewall or hosting panel.

### `secret`

```yml
secret: "CHANGE_ME"
```

This is the main security token for the API.

If left as `CHANGE_ME`, the plugin will automatically generate a random secret and save it.

Use a long random string, for example:

```yml
secret: "4NfJz9LxA1vQp8YeK2tRm7DsC0uBw3Hg"
```

Do not share this token publicly.

### `log-buffer-size`

```yml
log-buffer-size: 200
```

This is the size of the in-memory recent log buffer.

## Plugin commands

### `/applebridge reload`

Reloads the config and restarts the HTTP API.

### `/applebridge status`

Shows:

- whether the API is enabled
- whether the HTTP server is running
- which port is being used
- current log buffer size

## API

## `POST /execute`

Executes a command from the Minecraft console.

Example JSON body:

```json
{
  "command": "say hello"
}
```

Required header:

```text
Authorization: YOUR_SECRET
```

If successful, the API returns:

```text
OK
```

If the token is wrong:

- `403 Forbidden`

If the JSON is invalid:

- `400 Invalid JSON`

## `GET /logs`

Returns recent server log lines.

Example:

```text
GET /logs?limit=20
```

Notes:

- `limit` is optional
- default is `50`
- maximum is `200`

Example response:

```text
[23:04:10] [INFO] [Minecraft] Done (12.345s)! For help, type "help"
[23:05:22] [INFO] [Minecraft] Player123 joined the game
[23:06:16] [INFO] [Minecraft] [Server] hello from api
```

## Testing with PowerShell

### Execute a command

```powershell
Invoke-RestMethod -Uri 'http://127.0.0.1:8080/execute' `
  -Method POST `
  -Headers @{ Authorization = 'YOUR_SECRET' } `
  -ContentType 'application/json' `
  -Body '{"command":"say hello from api"}'
```

### Read recent logs

```powershell
Invoke-RestMethod -Uri 'http://127.0.0.1:8080/logs?limit=20' `
  -Method GET `
  -Headers @{ Authorization = 'YOUR_SECRET' }
```

## Testing on Linux with `curl`

### Execute a command

```bash
curl -X POST http://127.0.0.1:8080/execute \
  -H "Authorization: YOUR_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"command":"say hello from linux"}'
```

### Read recent logs

```bash
curl http://127.0.0.1:8080/logs?limit=20 \
  -H "Authorization: YOUR_SECRET"
```

### If your bot runs on another machine

```bash
curl -X POST http://YOUR_SERVER_IP:8080/execute \
  -H "Authorization: YOUR_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"command":"list"}'
```

## Connecting a Discord bot

AppleBridge itself does not include a Discord bot.  
Your bot should send HTTP requests to the plugin.

The bot needs:

- the API URL
- the secret token

Example environment values:

```env
APPLEBRIDGE_URL=http://127.0.0.1:8080/execute
APPLEBRIDGE_SECRET=YOUR_SECRET
```

If the bot runs on another machine:

```env
APPLEBRIDGE_URL=http://YOUR_SERVER_IP:8080/execute
APPLEBRIDGE_SECRET=YOUR_SECRET
```

## Security recommendations

Because this plugin can execute console commands, security matters a lot.

You should always:

- use a strong random secret
- never publish the secret
- restrict command access in your bot
- avoid exposing the API publicly unless necessary

If possible:

- run the bot and Minecraft on the same machine
- use `127.0.0.1`
- restrict firewall access by IP

For Linux servers, also check:

- `ufw`, `iptables`, or `firewalld`
- your hosting provider firewall
- Docker or panel port mapping

If you expose the API outside the machine, make sure:

- the AppleBridge port is actually opened in your hosting panel
- the same port is allowed in the Linux firewall
- Docker or panel port forwarding points to the correct internal port

## Common issues

## The plugin does not load

Check:

- that you are using Paper or a Paper-compatible server
- that your Java version matches your server version
- the server logs for startup errors

## The bot gets `403 Forbidden`

Usually this means:

- wrong `Authorization` header
- extra spaces in the token
- secret does not match `config.yml`

## The bot gets a timeout

Check:

- correct IP or domain
- correct port
- whether the port is open
- whether the server is running
- whether `/applebridge status` shows `running: true`

## Commands do not execute

Check:

- that the command itself is valid
- that it works from the server console manually

## Example commands

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

## License

This project is released under the `Apache License 2.0`.

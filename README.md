
---

# OTT Application

## Description

This part of the project implements an OTT (over-the-top) TV system over the internet. It consists of a simple backend server and an Android TV application integrated with Live Channels.

The backend provides channel data, stream URLs, and program information, while the Android app fetches that data, stores it in the system, and handles playback.

---

## Running the server

The backend is implemented using FastAPI.

```bash
uvicorn server:app --host 0.0.0.0 --port 8000
```

The server must be running before scanning channels in the app.

---

## API

The application uses the following endpoints:

* `GET /channels` – list of channels
* `GET /meta/{id}` – channel details
* `GET /stream/{id}` – returns HLS (.m3u8) stream URL
* `GET /programs/{id}` – daily program (EPG)
* `GET /epg` – all channels and programs

The EPG is generated dynamically for a 24-hour period and does not use a database.

---

## Android application

When started, the user can trigger channel scanning.

The process:

* fetch channel list from the server
* remove existing channels from the system
* insert new channels into TvContract
* fetch and store program data for each channel

All network operations run on background threads.

---

## Playback

The app uses TvInputService to integrate with Live Channels.

On channel change:

* the player is reset
* content rating is checked
* the stream is loaded
* playback starts

Streams are in HLS (.m3u8) format.

---

## Parental control

The app follows system parental control settings.

If content is restricted:

* playback is stopped
* the content is blocked

---

## Notes

* Use `10.0.2.2:8000` to access the server from the emulator
* HTTP timeouts: 10s connect, 15s read
* EPG is refreshed periodically in the background

---

---

## Author

Nina Dragićević and
Bogdan Cvetanovski Pašalić,
Faculty of Technical Sciences, Novi Sad
Department of Computer Engineering and Communications

---


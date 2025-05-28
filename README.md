# API – Personal Spring Boot Services

This repository houses a collection of custom **Spring Boot** APIs deployed to a **self-hosted server** and exposed via **Cloudflare Tunnels** at [api.jamesonzeller.com](https://api.jamesonzeller.com). Each endpoint serves a unique purpose, ranging from personal productivity to hobby projects, all built with performance, security, and maintainability in mind.

---

## ✨ Features

- Written in **Kotlin** using **Spring Boot**
- **Dockerized** and deployed on a self-hosted **TrueNAS** server
- Reverse-proxied through **Cloudflare Tunnels**
- Fully **idempotent** and adheres to Spring Boot best practices
- Includes **unit tests** for core logic and controllers

---

## 🔌 Public Endpoints

### `GET /get_current_read`
Fetches the current book being read from Goodreads.  
**Returns:** `{"currentRead": "Book Title by Author Name"}`

### `GET` & `POST /generate_word_search`
Generates a randomized word search puzzle based on input or default word list.
- `GET`: Returns a default puzzle
- `POST`: Accepts JSON input of custom words  
  **Returns:** `{"search": [...], "words": [...]}`

---

## 🔒 Private Endpoints

These endpoints are used by a **smart mirror project** running in my home environment.

### `GET /get_upcoming_events`
Fetches upcoming Google Calendar events, formatted for display.

### `GET /get_notion_tasks`
Retrieves active to-do items from a Notion database.

> ⚠️ These routes are secured and accessible only from internal services.

---

## 🧠 Planned Endpoints for Tasklight Integration

These endpoints will serve the [Tasklight](https://jamesonzeller.com/tasklight) productivity app:

### `POST /track_usage`
Logs user activity and AI usage counts.

### `GET /get_quota`
Returns remaining free-tier quota or subscription status.

---

## 🧪 Testing

All major services and controllers include **unit tests** using **Spring Boot Test** and **MockMvc**.  
Tests cover:
- Controller logic
- Service validation
- Input edge cases

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **Framework:** Spring Boot
- **Deployment:** Docker, TrueNAS, Cloudflare Tunnel
- **Testing:** JUnit 5, Spring Boot Test
- **Other:** Flask microservices, Notion API, Google Calendar API, Goodreads scraping

---

## 📁 Explore More Projects

Check out my other work at:  
🔗 [github.com/imjamesonzeller](https://github.com/imjamesonzeller)
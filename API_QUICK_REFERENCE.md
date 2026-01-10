# 📮 REST API Quick Reference Card

## 🔗 Base URL
```
http://localhost:8080
```

---

## 1️⃣ StickerCharacter API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/sticker-characters` | Get all |
| GET | `/api/sticker-characters/page` | Get all (paginated) |
| GET | `/api/sticker-characters/{id}` | Get by ID |
| GET | `/api/sticker-characters/name/{name}` | Get by name |
| GET | `/api/sticker-characters/category/{category}` | Get by category |
| POST | `/api/sticker-characters` | Create new |
| PUT | `/api/sticker-characters/{id}` | Update |
| DELETE | `/api/sticker-characters/{id}` | Delete |

**POST/PUT Body:**
```json
{
  "name": "Cute Cat",
  "category": ["animal", "cat", "cute"]
}
```

---

## 2️⃣ CharacterExpression API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/character-expressions` | Get all |
| GET | `/api/character-expressions/page` | Get all (paginated) |
| GET | `/api/character-expressions/{id}` | Get by ID |
| GET | `/api/character-expressions/character/{characterId}` | Get by character |
| GET | `/api/character-expressions/category/{category}` | Get by category |
| POST | `/api/character-expressions` | Create new |
| PUT | `/api/character-expressions/{id}` | Update |
| DELETE | `/api/character-expressions/{id}` | Delete |

**POST/PUT Body:**
```json
{
  "expression": "happy",
  "category": ["emotion", "positive"],
  "stickerCharacterId": 1
}
```

---

## 3️⃣ PromptAI API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/prompts` | Get all |
| GET | `/api/prompts/page` | Get all (paginated) |
| GET | `/api/prompts/{id}` | Get by ID |
| GET | `/api/prompts/type/{type}` | Get by type |
| GET | `/api/prompts/category/{category}` | Get by category |
| GET | `/api/prompts/type/{type}/category/{category}` | Get by type & category |
| POST | `/api/prompts` | Create new |
| PUT | `/api/prompts/{id}` | Update |
| DELETE | `/api/prompts/{id}` | Delete |

**POST/PUT Body:**
```json
{
  "prompt": "Create a cute cat sticker",
  "category": ["sticker", "animal"],
  "type": "sticker_prompt"
}
```

---

## 🎯 cURL Examples

### Create Character
```bash
curl -X POST http://localhost:8080/api/sticker-characters \
  -H "Content-Type: application/json" \
  -d '{"name":"Cute Cat","category":["animal","cat","cute"]}'
```

### Get All (Paginated)
```bash
curl "http://localhost:8080/api/sticker-characters/page?page=0&size=20"
```

### Get by ID
```bash
curl http://localhost:8080/api/sticker-characters/1
```

### Update
```bash
curl -X PUT http://localhost:8080/api/sticker-characters/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Super Cute Cat","category":["animal","cat"]}'
```

### Delete
```bash
curl -X DELETE http://localhost:8080/api/sticker-characters/1
```

---

## 📊 Response Codes

| Code | Status | Meaning |
|------|--------|---------|
| 200 | OK | Success |
| 201 | Created | Resource created |
| 204 | No Content | Delete success |
| 400 | Bad Request | Invalid request |
| 404 | Not Found | Not found |
| 500 | Server Error | Server error |

---

## 💡 Tips
- Always create **StickerCharacter** before **CharacterExpression**
- Use pagination: `?page=0&size=20`
- Content-Type: `application/json` for POST/PUT
- Save IDs from create responses

---

**Total Endpoints: 25** | **Version: 1.0.0** | **Last Updated: 2026-01-03**


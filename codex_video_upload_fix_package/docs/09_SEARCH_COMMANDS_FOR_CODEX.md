# 09 — Suggested Repository Searches

Các command dưới đây chỉ để discovery; Codex điều chỉnh theo OS/repo.

```bash
rg -n -i "add video|video url|videoUrl|video_url|duration|youtube|youtu\.be"
rg -n -i "MultipartFile|multipart|spring\.servlet\.multipart"
rg -n -i "@PostMapping|@PutMapping|@PatchMapping|@DeleteMapping"
rg -n -i "FormData|fetch\(|axios|XMLHttpRequest"
rg -n -i "csrf|X-XSRF-TOKEN|_csrf"
rg -n -i "iframe|<video|video-js|youtube"
rg -n -i "estimated.*duration|total.*duration|completion.*time"
rg -n -i "LessonType|lesson_type|VIDEO"
```

Nếu Maven:
```bash
./mvnw test
```

Nếu Gradle:
```bash
./gradlew test
```

Nếu frontend có package.json:
```bash
npm test
npm run build
```

Chỉ chạy command tồn tại trong repository.

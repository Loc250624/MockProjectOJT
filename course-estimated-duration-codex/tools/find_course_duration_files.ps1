$patterns = @(
    "class Course",
    "class Lesson",
    "videoUrl",
    "videoDuration",
    "durationSeconds",
    "lessonType",
    "VIDEO",
    "CourseDto",
    "CourseResponse",
    "course-card",
    "course-detail",
    "my-courses"
)

Write-Host "Searching project files related to course video duration..." -ForegroundColor Cyan

Get-ChildItem -Path "src" -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object {
        $_.Extension -in @(".java", ".html", ".js", ".css", ".sql", ".xml", ".properties", ".yml", ".yaml")
    } |
    Select-String -Pattern $patterns -SimpleMatch |
    Select-Object Path, LineNumber, Line |
    Sort-Object Path, LineNumber |
    Format-Table -AutoSize

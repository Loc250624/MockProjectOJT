# 07 — Regression Checklist

Codex phải chạy kiểm tra các vùng liên quan gián tiếp tới video.

## Lesson management
- create lesson
- edit lesson title/content
- reorder lesson
- delete lesson
- switch lesson type nếu chức năng hiện có hỗ trợ
- lesson list pagination/filter

## Course
- course edit
- publish/unpublish
- course detail
- total lesson count
- estimated completion duration

## Student
- enrollment gate
- open first/free lesson nếu project đang có
- video playback
- progress update after viewing
- next/previous lesson navigation

## Teacher/Admin
- teacher can only manage own permitted course
- admin views không lỗi khi encounter sourceType mới
- serialization/list DTO không null pointer

## Shared frontend
- layout/header
- modal stack/z-index
- dark/light theme
- mobile
- toast/confirm
- no JS global exception that breaks other buttons

## Backend
- startup migrations
- repository queries/projections
- Jackson serialization
- validation
- exception handler
- security config
- static resource mapping/media controller

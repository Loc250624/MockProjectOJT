# Implementation Rules

## Controller

- Controller chỉ điều phối.
- Không đặt query/aggregation phức tạp trong controller.
- Không truyền entity trực tiếp nếu gây lazy loading hoặc lộ dữ liệu.
- Validate request/filter.
- Dùng authenticated principal cho teacher identity.

## Service

- Chứa business rule.
- Một định nghĩa doanh thu thống nhất.
- Xử lý date/timezone.
- Trả DTO rõ ràng.
- Có test.

## Repository

- Query đúng trạng thái.
- Filter teacher ownership.
- Tránh join gây double count.
- Dùng projection nếu dashboard chỉ cần aggregate.
- Đặt tên method/query có ý nghĩa.

## Frontend

- Không hard-code dữ liệu.
- Không sửa global layout nếu không cần.
- Không tạo CSS selector quá rộng.
- Xử lý null/empty/loading/error.
- Format currency/date nhất quán.
- Escape output.
- Không tin dữ liệu client.

## Testing

Ưu tiên:
1. Repository slice test cho aggregation.
2. Service unit/integration test cho business rule.
3. Controller/Security test cho route và role.
4. Manual UI hoặc browser test.

Mỗi bug logic phải có test chống tái phát khi khả thi.

## Minimal diff

- Không rename hàng loạt.
- Không format toàn repository.
- Không thay dependency không liên quan.
- Không di chuyển package nếu không bắt buộc.

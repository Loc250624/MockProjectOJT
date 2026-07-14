# Backend & Data Validation Rules

## Revenue

Ưu tiên một nguồn sự thật duy nhất tại service/repository.

Cần xác định và test:
- trạng thái thành công;
- timestamp dùng để group;
- gross/net;
- discount;
- refund;
- multi-item order;
- currency;
- distinct order/student;
- quyền sở hữu course.

Không tính doanh thu bằng JavaScript.

## Date range

- Dùng kiểu ngày/giờ phù hợp.
- Xác định timezone hệ thống.
- Convert boundary một lần tại service.
- Tránh dùng `DATE(column)` nếu làm mất index trên bảng lớn, trừ khi dự án nhỏ hoặc có lý do.
- From > To phải trả validation error.
- Giới hạn khoảng ngày nếu query quá nặng.

## Aggregation

- `COUNT(DISTINCT ...)` khi cần.
- Không join gây nhân bản bản ghi.
- So sánh tổng raw query với tổng grouped query.
- Với month phải group cả year và month.
- Với teacher phải filter ownership ở query/service.

## Active student

Không tự suy đoán. Chọn một trong các nguồn đã tồn tại:
1. lastLoginAt;
2. activity/audit log;
3. lesson progress updatedAt;
4. enrollment activity;
5. submission/quiz activity.

Ghi rõ định nghĩa trong UI/report.

## System settings

Khuyến nghị:
- key unique;
- value;
- type;
- category;
- description;
- editable;
- sensitive;
- updatedAt;
- updatedBy;
- version.

Nếu project hiện tại đơn giản hơn, giữ kiến trúc cũ nhưng vẫn phải:
- whitelist key;
- validate type;
- không expose secret;
- xử lý key thiếu bằng default an toàn;
- invalidate cache nếu có.

## Error handling

Không trả 500 cho input sai.
Cần phân biệt:
- 400 validation;
- 401 unauthenticated;
- 403 unauthorized;
- 404 missing resource;
- 409 conflict;
- 500 unexpected.

## Security

- Không nhận teacherId từ client nếu có thể lấy từ authenticated principal.
- Admin settings phải có method-level hoặc route-level authorization.
- Không log password, token, secret, full payment payload.
- Không render stack trace ra UI.

# AI Chatbot Global – Codex Change Kit

Gói này dùng để giao cho Codex chỉnh sửa tính năng **AI Tutor** hiện có trong dự án LUmiNa/OJTSU26 E-Learning thành **AI Chatbot** dùng được trên toàn bộ website.

## Kết quả bắt buộc

1. Đổi tên hiển thị từ **AI Tutor** thành **AI Chatbot**.
2. Không hiển thị tên lesson bên dưới tiêu đề chatbot.
3. Tin nhắn chào của bot xuất hiện trước nhóm câu hỏi nhanh.
4. Câu hỏi nhanh chỉ xuất hiện một lần trong mỗi cuộc hội thoại.
5. Chatbot trả lời theo ngữ cảnh toàn website, không còn bị khóa vào riêng một lesson.
6. Icon mở chatbot được mount tại layout dùng chung và xuất hiện trên tất cả trang.
7. Không làm lộ dữ liệu trái quyền, không cho chatbot tự thay đổi điểm, tiến độ, thanh toán hoặc dữ liệu người khác.
8. Không sửa các module không liên quan và không phá vỡ API/tính năng AI Tutor đang hoạt động.

## Cách dùng

1. Giải nén thư mục này vào root của repository hoặc mở song song với repository.
2. Đưa nội dung `00_CODEX_MASTER_PROMPT.md` cho Codex.
3. Cho Codex đọc các file yêu cầu theo thứ tự từ `01` đến `12`.
4. `prototype/` là bản tham chiếu hành vi giao diện, không phải file bắt buộc phải chép nguyên vào dự án.
5. `samples/` là mẫu hợp đồng và test. Codex phải thích nghi với stack, package, naming và kiến trúc thật của repository.
6. Sau khi sửa, Codex phải điền `12_CHANGE_REPORT_TEMPLATE.md` và chạy checklist trong `10_ACCEPTANCE_CRITERIA.md`.

## Tài liệu tham chiếu

- `assets/current-ai-tutor-design.png`: ảnh trạng thái giao diện hiện tại do người dùng cung cấp.
- `prototype/ai-chatbot-desired.html`: mockup chạy độc lập, thể hiện đúng thứ tự greeting → quick actions → conversation.

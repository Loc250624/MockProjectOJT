# Implementation guardrails

## Không được làm

- Không sao chép footer FPT Shop theo pixel.
- Không thêm QR code, badge thanh toán, logo mạng xã hội hoặc chứng nhận giả.
- Không tự bịa email, số điện thoại, địa chỉ hoặc pháp nhân.
- Không dùng link thật cho các mục footer.
- Không dùng `href="#"`.
- Không đặt `cursor: pointer` cho mục không click.
- Không thêm package mới.
- Không thay đổi cấu trúc routing.
- Không chỉnh các biến CSS toàn cục nếu có thể làm các trang khác đổi giao diện.
- Không sửa backend/database.
- Không tăng chiều cao desktop chỉ để nhét nội dung theo chiều dọc.
- Không dùng fixed height gây cắt chữ ở mobile.

## Nên làm

- Reuse logo/brand name từ component Header hoặc config chung.
- Reuse container, typography và color token có sẵn.
- Dùng mảng dữ liệu để render các cột.
- Dùng CSS Grid hoặc utility grid phù hợp với codebase.
- Giữ thay đổi nhỏ và dễ review.
- Chụp hoặc mô tả kết quả ở 4 kích thước màn hình.
- Chạy build và lint sau khi sửa.

# QA checklist

## Functional

- [ ] `Ready to begin?` đã biến mất khỏi khu vực cuối trang.
- [ ] `Browse Courses` và `Create Account` không còn nằm trong footer.
- [ ] Click vào từng nhãn footer không đổi URL.
- [ ] Click vào từng nhãn footer không cuộn về đầu trang.
- [ ] Không có tab mới được mở.
- [ ] Không có lỗi console do handler/link giả.
- [ ] Copyright dùng năm hiện tại tự động.

## Visual desktop

- [ ] Footer chiếm toàn bộ chiều ngang như section cũ.
- [ ] Nội dung được phân bổ đều theo chiều ngang.
- [ ] Không có vùng trống lớn không có mục đích.
- [ ] Tổng chiều cao gần bằng khu vực CTA/footer cũ.
- [ ] Màu sắc, font và spacing đồng bộ với website.
- [ ] Không có yếu tố nhận diện của FPT Shop.
- [ ] Chatbot/floating action không che nội dung footer.

## Responsive

- [ ] 1440 px: 4 cột rõ ràng, không cắt nội dung.
- [ ] 1024 px: bố cục hợp lý, không chồng chữ.
- [ ] 768 px: chuyển 2 cột hoặc bố cục tương đương.
- [ ] 390 px: 1 cột, không horizontal scroll.
- [ ] Text dài xuống dòng tự nhiên.
- [ ] Footer không tràn khỏi viewport.

## Regression

- [ ] Header và các section phía trên không thay đổi.
- [ ] Không thay đổi routing/authentication.
- [ ] Không có file backend/database bị sửa.
- [ ] Lint pass.
- [ ] Build pass.
- [ ] Git diff chỉ chứa file liên quan đến footer.

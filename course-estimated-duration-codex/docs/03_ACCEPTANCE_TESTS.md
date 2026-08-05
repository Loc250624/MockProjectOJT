# Acceptance Tests

| ID | Dữ liệu | Kết quả |
|---|---|---|
| AT-01 | Course A có video 10, 15, 20 phút | 45 phút |
| AT-02 | Course A có 2 video và 1 quiz | Chỉ tổng 2 video |
| AT-03 | Course A 30 phút, Course B 50 phút | A=30, B=50 |
| AT-04 | Không có video | 0 giây, `Chưa có thời lượng video` |
| AT-05 | Duration null, 0, -10 | Không cộng |
| AT-06 | Video lesson inactive/deleted | Không cộng |
| AT-07 | Một lesson có 3 resource records | Duration chỉ cộng 1 lần |
| AT-08 | 20 course trong catalog | Không có 20 query duration riêng |
| AT-09 | Teacher sửa duration | Reload course cho kết quả mới |
| AT-10 | Mobile course card | Không overflow, metadata vẫn đọc được |

## Formatter cases

| Seconds | Display |
|---:|---|
| 0 | Chưa có thời lượng video |
| 1 | < 1 phút |
| 59 | < 1 phút |
| 60 | 1 phút |
| 61 | 2 phút |
| 3540 | 59 phút |
| 3599 | 1 giờ |
| 3600 | 1 giờ |
| 3660 | 1 giờ 1 phút |
| 7200 | 2 giờ |

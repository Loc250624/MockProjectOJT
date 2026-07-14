# Code Review Checklist

## Scope
- [ ] Chỉ sửa ANL-01..04 và SYS-01 hoặc dependency trực tiếp.
- [ ] Không có file generated/binary.
- [ ] Không có secret.

## Logic
- [ ] Revenue status đúng.
- [ ] Teacher ownership đúng.
- [ ] Không double count.
- [ ] Date/timezone đúng.
- [ ] Active student có định nghĩa.
- [ ] Settings có validation.

## Security
- [ ] Anonymous bị chặn.
- [ ] Sai role bị chặn.
- [ ] Không IDOR.
- [ ] CSRF đúng.
- [ ] Secret không lộ.

## UI
- [ ] Đúng layout.
- [ ] Active menu.
- [ ] Loading/empty/error.
- [ ] KPI/chart/table khớp.
- [ ] Responsive.
- [ ] Không console error.

## Tests
- [ ] Build pass.
- [ ] Unit/integration pass.
- [ ] Manual test pass.
- [ ] Regression pass.

# UI Copy & States

Dùng wording gần với design hiện tại, không cần redesign.

## User actions

### Reset Password — LOCAL
Button:
`Reset Password`

Modal title:
`Generate Password Reset Link`

Body:
`Create a one-time password reset link for this user. The link will expire automatically.`

Success:
`Password reset link created successfully.`

### Reset Password — OAuth
Disabled/help text:
`Password is managed by Google.`
hoặc
`Password is managed by GitHub.`

### Change Role
Modal title:
`Change User Role`

Confirmation:
`Change role from STUDENT to TEACHER?`

Success:
`User role updated successfully.`

### Block
Modal title:
`Block User Account`

Body:
`This user will no longer be able to sign in. Existing account history will be preserved.`

Success:
`User account blocked.`

### Unblock
Success:
`User account unblocked.`

### Soft-delete
Modal title:
`Soft-delete User Account`

Body:
`This disables the account while preserving payment, enrollment, and audit history.`

Danger confirmation should require a deliberate confirm click, but do not require typing the username unless the design system already uses that pattern.

## Categories

### Used category delete conflict
Title:
`Category is in use`

Body:
`This category is assigned to {count} course(s). Reassign the courses before deleting the category.`

If reassign flow exists:
- dropdown: `Move courses to`
- primary: `Reassign & Delete`
- secondary: `Cancel`

If policy forbids deletion of in-use category:
- primary: `View affected courses`
- secondary: `Close`

## Transactions

Examples:
- `47.933.750 ₫`
- `5.499.750 ₫`
- `2.999.750 ₫`

Do not show:
- `47,933,750.00 USD`
- `5,499,750.00 USD`

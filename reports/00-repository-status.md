# 00 - Repository Status

## Scope

Task: check repository status and prepare environment for ANL-01, ANL-02, ANL-03, ANL-04, SYS-01.

No source code, database, branch, stash, reset, checkout, commit, or deletion was performed.

## Documents Read

Root-level check:
- `AGENTS.md`: requested; currently reported by Git as deleted in the working tree.
- `README_VI.md`: not found at repository root.
- `01_MASTER_PROMPT.md`: not found at repository root.
- `02_REPOSITORY_DISCOVERY.md`: not found at repository root.
- `03_FEATURE_TEST_MATRIX.md`: not found at repository root.
- `04_UI_UX_ACCEPTANCE_CRITERIA.md`: not found at repository root.
- `05_BACKEND_DATA_VALIDATION.md`: not found at repository root.
- `06_TEST_EXECUTION_RUNBOOK.md`: not found at repository root.
- `08_IMPLEMENTATION_RULES.md`: not found at repository root.

Untracked maintenance kit documents read:
- `Codex_ANL_SYS_Maintenance_Kit/AGENTS.md`
- `Codex_ANL_SYS_Maintenance_Kit/README_VI.md`
- `Codex_ANL_SYS_Maintenance_Kit/01_MASTER_PROMPT.md`
- `Codex_ANL_SYS_Maintenance_Kit/02_REPOSITORY_DISCOVERY.md`
- `Codex_ANL_SYS_Maintenance_Kit/03_FEATURE_TEST_MATRIX.md`
- `Codex_ANL_SYS_Maintenance_Kit/04_UI_UX_ACCEPTANCE_CRITERIA.md`
- `Codex_ANL_SYS_Maintenance_Kit/05_BACKEND_DATA_VALIDATION.md`
- `Codex_ANL_SYS_Maintenance_Kit/06_TEST_EXECUTION_RUNBOOK.md`
- `Codex_ANL_SYS_Maintenance_Kit/08_IMPLEMENTATION_RULES.md`

## Git State

Branch current:
- `feature/anl-and-sys-maintainance`

Latest commit:
- `f8cb855 Apply BCrypt migration: use standard BCryptPasswordEncoder with PasswordMigrationRunner`

Recent commits:
- `f8cb855 Apply BCrypt migration: use standard BCryptPasswordEncoder with PasswordMigrationRunner`
- `7a71b51 Merge remote-tracking branch 'origin/feature/blog-and-schedule-fix' into develop`
- `d78c88a Fix AssessmentService compiler errors and test order query method calls`
- `b3416b2 Merge branch origin/feature/assessment into develop`
- `6a727f5 Merge branch origin/feature/blog into develop`

## Changed Files

Tracked changes not staged:
- `AGENTS.md` - deleted in working tree.
- `src/main/resources/application.properties` - modified.

Untracked files/directories:
- `Codex_ANL_SYS_Maintenance_Kit/`
- `reports/00-repository-status.md`

Expanded untracked maintenance kit contents:
- `Codex_ANL_SYS_Maintenance_Kit/01_MASTER_PROMPT.md`
- `Codex_ANL_SYS_Maintenance_Kit/02_REPOSITORY_DISCOVERY.md`
- `Codex_ANL_SYS_Maintenance_Kit/03_FEATURE_TEST_MATRIX.md`
- `Codex_ANL_SYS_Maintenance_Kit/04_UI_UX_ACCEPTANCE_CRITERIA.md`
- `Codex_ANL_SYS_Maintenance_Kit/05_BACKEND_DATA_VALIDATION.md`
- `Codex_ANL_SYS_Maintenance_Kit/06_TEST_EXECUTION_RUNBOOK.md`
- `Codex_ANL_SYS_Maintenance_Kit/07_BUG_REPORT_TEMPLATE.md`
- `Codex_ANL_SYS_Maintenance_Kit/08_IMPLEMENTATION_RULES.md`
- `Codex_ANL_SYS_Maintenance_Kit/09_MANUAL_TEST_CASES.md`
- `Codex_ANL_SYS_Maintenance_Kit/10_CODE_REVIEW_CHECKLIST.md`
- `Codex_ANL_SYS_Maintenance_Kit/AGENTS.md`
- `Codex_ANL_SYS_Maintenance_Kit/MANIFEST.md`
- `Codex_ANL_SYS_Maintenance_Kit/README_VI.md`
- `Codex_ANL_SYS_Maintenance_Kit/START_HERE.txt`
- `Codex_ANL_SYS_Maintenance_Kit/prompts/ANL-01.md`
- `Codex_ANL_SYS_Maintenance_Kit/prompts/ANL-02.md`
- `Codex_ANL_SYS_Maintenance_Kit/prompts/ANL-03.md`
- `Codex_ANL_SYS_Maintenance_Kit/prompts/ANL-04.md`
- `Codex_ANL_SYS_Maintenance_Kit/prompts/REGRESSION.md`
- `Codex_ANL_SYS_Maintenance_Kit/prompts/SYS-01.md`
- `Codex_ANL_SYS_Maintenance_Kit/scripts/unix/run-verification.sh`
- `Codex_ANL_SYS_Maintenance_Kit/scripts/windows/run-verification.ps1`
- `Codex_ANL_SYS_Maintenance_Kit/sql/verification_queries_template.sql`
- `Codex_ANL_SYS_Maintenance_Kit/templates/FINAL_REPORT_TEMPLATE.md`
- `Codex_ANL_SYS_Maintenance_Kit/templates/ROUTE_INVENTORY.csv`
- `Codex_ANL_SYS_Maintenance_Kit/templates/TEST_RESULT.csv`

## Conflict Check

Unresolved conflict files:
- None detected by `git diff --name-only --diff-filter=U`.

## Branch Suitability

The branch name `feature/anl-and-sys-maintainance` appears suitable for ANL/SYS maintenance work by intent. Note: `maintainance` appears misspelled, but this does not block work by itself.

## Tracked Secret Check

Tracked sensitive config files detected:
- `src/main/resources/application.properties`
- `src/main/resources/application.secret.properties.example`

Sensitive keys found in tracked `src/main/resources/application.properties`:
- `spring.datasource.password` - environment placeholder.
- `spring.security.oauth2.client.registration.google.client-secret` - environment placeholder.
- `spring.security.oauth2.client.registration.github.client-secret` - environment placeholder.
- `payment.gateway.momo.secretKey` - literal value present.
- `payment.gateway.vnpay.hashSecret` - literal value present.

Values were intentionally not printed in this report.

## Risks Before Starting

- Repository is not clean before implementation.
- Root `AGENTS.md` is deleted in the working tree, which may remove project-specific Codex rules if committed accidentally.
- `src/main/resources/application.properties` has an existing local modification unrelated to this status report.
- `src/main/resources/application.properties` is tracked and contains payment gateway secret keys with literal values.
- The ANL/SYS maintenance kit is untracked, so its prompts/scripts/reports templates are not yet part of committed project history.
- Starting implementation now could mix new ANL/SYS changes with pre-existing local changes and make review or rollback harder.

## Conclusion

Do not start source-code implementation yet.

It is reasonable to continue only after the repository owner confirms how to handle the pre-existing deleted/modified files and the tracked literal secret values in `application.properties`. The current branch itself appears appropriate for ANL/SYS work.

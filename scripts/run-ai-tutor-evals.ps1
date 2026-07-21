param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$CasesPath = "evals/ai_tutor_eval_cases.jsonl",
    [string]$Cookie,
    [string]$CsrfToken,
    [string]$CsrfHeader = "X-XSRF-TOKEN",
    [int]$LessonId = 0
)

if ([string]::IsNullOrWhiteSpace($Cookie)) {
    throw "Cookie is required. Copy the authenticated student's Cookie header from the browser."
}
if ([string]::IsNullOrWhiteSpace($CsrfToken)) {
    throw "CsrfToken is required. Use the XSRF-TOKEN cookie value or the page CSRF holder."
}

$passed = 0
$failed = 0
$results = @()

Get-Content -LiteralPath $CasesPath | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object {
    $case = $_ | ConvertFrom-Json
    if ($LessonId -gt 0) {
        $case.lessonId = $LessonId
    }
    $body = @{
        lessonId = $case.lessonId
        message = $case.message
        action = $case.action
        history = @()
    } | ConvertTo-Json -Depth 6

    try {
        $response = Invoke-RestMethod `
            -Method Post `
            -Uri "$BaseUrl/api/student/ai-tutor/chat" `
            -Headers @{ Cookie = $Cookie; $CsrfHeader = $CsrfToken } `
            -ContentType "application/json" `
            -Body $body
        $actualRefused = [bool]$response.data.refused
        $ok = $actualRefused -eq [bool]$case.expectedRefused
        if ($ok) { $passed++ } else { $failed++ }
        $results += [pscustomobject]@{
            id = $case.id
            expectedRefused = [bool]$case.expectedRefused
            actualRefused = $actualRefused
            ok = $ok
            reasonCode = $response.data.reasonCode
        }
    } catch {
        $failed++
        $results += [pscustomobject]@{
            id = $case.id
            expectedRefused = [bool]$case.expectedRefused
            actualRefused = $null
            ok = $false
            reasonCode = "HTTP_ERROR"
        }
    }
}

$results | Format-Table -AutoSize
"Pass rate: {0}/{1}" -f $passed, ($passed + $failed)

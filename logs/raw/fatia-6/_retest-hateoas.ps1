$ErrorActionPreference = "Stop"
$http = "C:\Users\zuria\AppData\Roaming\Python\Python313\Scripts\http.exe"
$raw = "c:\Users\zuria\CODE\TCC\secretariaonline2\secretariaOnline2\logs\raw\fatia-6"
$env:PYTHONUTF8 = "1"
$cursoId = "01a05940-ec36-75cc-ab53-d999ce0a7fa1"
$session = "--session-read-only=f6-re2-prof"

Write-Host "=== login ==="
& $http --session=f6-re2-prof --print=hb --timeout=20 POST http://localhost:8080/auth/login identificador=prof.ana@ufpr.br senha=ProfS3nh@Forte! | Out-File "$raw\re-login-prof.txt" -Encoding utf8
Select-String -Path "$raw\re-login-prof.txt" -Pattern "HTTP/|status" | Select-Object -First 5

Write-Host "=== csrf ==="
$csrfRaw = & $http --session=f6-re2-prof --print=b --timeout=20 GET http://localhost:8080/auth/csrf
Set-Content "$raw\re-csrf.txt" $csrfRaw -Encoding utf8
$cp = ($csrfRaw | ConvertFrom-Json).token
Write-Host "csrf=$cp"

$now = [DateTimeOffset]::UtcNow
$inicio = $now.AddHours(-1).ToString("yyyy-MM-ddTHH:mm:ssZ")
$fim = $now.AddHours(4).ToString("yyyy-MM-ddTHH:mm:ssZ")
$obj = @{ titulo="Fatia6 retest HATEOAS"; descricao="AGENDADO deve ter abrir-janela-entrada"; idCurso=$cursoId; attendanceMode="SECRET_SINGLE"; chCreditadas=4.0; inicioEm=$inicio; fimEm=$fim } | ConvertTo-Json
Set-Content "$raw\_evt-retest.json" $obj -Encoding utf8

Write-Host "=== POST /events ==="
& $http --session=f6-re2-prof --print=hb --timeout=20 POST http://localhost:8080/events "X-XSRF-TOKEN:$cp" "@$raw\_evt-retest.json" | Out-File "$raw\re-evt-create.txt" -Encoding utf8
$id = [regex]::Match((Get-Content "$raw\re-evt-create.txt" -Raw), '"id"\s*:\s*"([0-9a-fA-F-]{36})"').Groups[1].Value
Write-Host "id=$id"

Write-Host "=== GET AGENDADO ==="
& $http --session=f6-re2-prof --print=hb --timeout=20 GET "http://localhost:8080/events/$id" | Out-File "$raw\re-evt-detail-agendado.txt" -Encoding utf8
Select-String -Path "$raw\re-evt-detail-agendado.txt" -Pattern "_links" | Select-Object -Last 1

Write-Host "=== GET CONCLUIDO 17fcf90f ==="
& $http --session=f6-re2-prof --print=hb --timeout=20 GET "http://localhost:8080/events/17fcf90f-f663-4865-aab0-cdf3e453eefc" | Out-File "$raw\re-evt-detail-concluido.txt" -Encoding utf8
Select-String -Path "$raw\re-evt-detail-concluido.txt" -Pattern "_links" | Select-Object -Last 1

Write-Host "DONE"

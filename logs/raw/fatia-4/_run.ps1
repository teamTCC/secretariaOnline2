$ErrorActionPreference = "Continue"
$http = "C:\Users\zuria\AppData\Roaming\Python\Python313\Scripts\http.exe"
$raw = "c:\Users\zuria\CODE\TCC\secretariaonline2\secretariaOnline2\logs\raw\fatia-4"
$env:PYTHONUTF8 = "1"
$cursoId = "01a05940-ec36-75cc-ab53-d999ce0a7fa1"
$now = [DateTimeOffset]::UtcNow
$inicio = $now.AddHours(-1).ToString("yyyy-MM-ddTHH:mm:ssZ")
$fim = $now.AddHours(4).ToString("yyyy-MM-ddTHH:mm:ssZ")
$results = New-Object System.Collections.Generic.List[object]
$idRe = '"id"\s*:\s*"([0-9a-fA-F-]{36})"'
$delRe = '"deliveryId"\s*:\s*"([0-9a-fA-F-]{36})"'
$hashRe = '"hashSha256"\s*:\s*"([0-9a-f]+)"'
$secretRe = '"secret"\s*:\s*"([^"]+)"'
$qrRe = '"qrToken"\s*:\s*"([^"]+)"'
$keyRe = '"storageKey"\s*:\s*"([^"]+)"'
$urlRe = '"uploadUrl"\s*:\s*"([^"]+)"'

function Dump([string]$name, [string[]]$httpArgs) {
  Write-Host ""
  Write-Host "===== $name ====="
  $out = & $http @httpArgs 2>&1 | Out-String
  Set-Content -Path (Join-Path $raw "$name.txt") -Value $out -Encoding utf8
  $status = "?"
  if ($out -match "HTTP/1\.[01] (\d+)") { $status = $Matches[1] }
  Write-Host "status=$status"
  $script:results.Add([pscustomobject]@{ name = $name; status = $status })
  return $out
}

Write-Host "login..."
& $http --session=f4-aluno --print=b POST http://localhost:8080/auth/login identificador=ana.aluno@ufpr.br "senha=AlunoS3nh@Forte!" | Out-Null
& $http --session=f4-prof --print=b POST http://localhost:8080/auth/login identificador=prof.ana@ufpr.br "senha=ProfS3nh@Forte!" | Out-Null
& $http --session=f4-sec --print=b POST http://localhost:8080/auth/login identificador=secretaria@ufpr.br "senha=SecrS3nh@Forte!" | Out-Null

$ca = (& $http --session=f4-aluno --print=b GET http://localhost:8080/auth/csrf | ConvertFrom-Json).token
$cp = (& $http --session=f4-prof --print=b GET http://localhost:8080/auth/csrf | ConvertFrom-Json).token
$cs = (& $http --session=f4-sec --print=b GET http://localhost:8080/auth/csrf | ConvertFrom-Json).token
$me = & $http --session=f4-aluno --print=b GET http://localhost:8080/me | ConvertFrom-Json
$alunoId = $me.id
Write-Host "alunoId=$alunoId"

Dump "faq" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/faq") | Out-Null
Dump "ticket-post" @("--session=f4-aluno","--print=hb","POST","http://localhost:8080/support/tickets","X-XSRF-TOKEN:$ca","assunto=Erro ao submeter atividade formativa","descricao=Ao tentar enviar o comprovante do minicurso, recebo erro 500. Teste fatia 4.") | Out-Null
Dump "ticket-mine" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/support/tickets/mine") | Out-Null

$pre = Dump "formativa-presign" @("--session=f4-aluno","--print=hb","POST","http://localhost:8080/formativas/comprovantes/presigned-url","X-XSRF-TOKEN:$ca","filename=comprovante.pdf","contentType=application/pdf")
$uploadUrl = $null
$storageKey = $null
if ($pre -match $urlRe) { $uploadUrl = $Matches[1] -replace '\\u0026','&' -replace '\\/','/' }
if ($pre -match $keyRe) { $storageKey = $Matches[1] }
Write-Host "storageKey=$storageKey"
$pdf = Join-Path $raw "tiny.pdf"
[IO.File]::WriteAllBytes($pdf, [byte[]](0x25,0x50,0x44,0x46,0x2D,0x31,0x2E,0x34,0x0A,0x25,0xE2,0xE3,0xCF,0xD3,0x0A))
if ($uploadUrl) {
  try {
    Invoke-WebRequest -Uri $uploadUrl -Method PUT -InFile $pdf -ContentType "application/pdf" -UseBasicParsing | Out-Null
    Set-Content (Join-Path $raw "formativa-put.txt") "PUT ok"
    $results.Add([pscustomobject]@{ name = "formativa-put"; status = "200" })
  } catch {
    Set-Content (Join-Path $raw "formativa-put.txt") $_.Exception.Message
    $results.Add([pscustomobject]@{ name = "formativa-put"; status = "err" })
  }
}
$form = @{
  titulo = "Palestra: Machine Learning Aplicado"
  descricao = "Participacao na palestra promovida pelo DINF em 2026-06-15"
  categoria = "PALESTRA"
  cargaHoraria = 4.0
  dataRealizacao = "2026-06-15"
  storageKeyComprovante = $storageKey
} | ConvertTo-Json
Set-Content (Join-Path $raw "_formativa.json") $form -Encoding utf8
Dump "formativa-post" @("--session=f4-aluno","--print=hb","POST","http://localhost:8080/formativas","X-XSRF-TOKEN:$ca",(Join-Path $raw "_formativa.json" | ForEach-Object { "@$_" })) | Out-Null
Dump "formativa-minhas" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/formativas/minhas") | Out-Null
Dump "formativa-resumo" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/formativas/resumo") | Out-Null

Dump "atend-agendar" @("--session=f4-aluno","--print=hb","POST","http://localhost:8080/me/service-records","X-XSRF-TOKEN:$ca","assunto=Revisao de matricula","descricao=Quero conferir disciplinas.","tipo=AGENDAMENTO") | Out-Null
$secOut = Dump "atend-sec" @("--session=f4-sec","--print=hb","POST","http://localhost:8080/service-records","X-XSRF-TOKEN:$cs","idAluno=$alunoId","assunto=Revisao de matricula","tipo=PRESENCIAL","descricao=Atendimento de balcao fatia 4.")
$srvId = $null
if ($secOut -match $idRe) { $srvId = $Matches[1] }
Dump "atend-me-pendente" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/me/service-records?status=PENDENTE_CIENCIA") | Out-Null
Dump "atend-alias" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/service-records?aluno=me&status=PENDENTE_CIENCIA") | Out-Null
if ($srvId) {
  Dump "atend-ack" @("--session=f4-aluno","--print=hb","POST","http://localhost:8080/service-records/$srvId/acknowledge","X-XSRF-TOKEN:$ca") | Out-Null
}

Dump "comm-publish" @("--session=f4-prof","--print=hb","POST","http://localhost:8080/communications","X-XSRF-TOKEN:$cp","titulo=Aviso da turma TADS fatia 4","conteudo=Prazo de formativas encerra sexta.","tipo=AVISO","cursoId=$cursoId") | Out-Null
$inboxOut = Dump "inbox" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/communications/me?page=0&size=20")
Dump "unread" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/communications/me/unread-count") | Out-Null
$deliveryId = $null
if ($inboxOut -match $delRe) { $deliveryId = $Matches[1] }
if ($deliveryId) {
  Dump "inbox-read" @("--session=f4-aluno","--print=hb","PATCH","http://localhost:8080/communications/deliveries/$deliveryId/read","X-XSRF-TOKEN:$ca") | Out-Null
  Dump "inbox-after-read" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/communications/me?page=0&size=20") | Out-Null
  Dump "unread-after" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/communications/me/unread-count") | Out-Null
}

function CreateEvent([string]$mode, [string]$title) {
  $body = @{
    titulo = $title
    descricao = "Evento harness fatia 4 $mode"
    idCurso = $cursoId
    attendanceMode = $mode
    chCreditadas = 4.0
    inicioEm = $inicio
    fimEm = $fim
  } | ConvertTo-Json
  $jf = Join-Path $raw "_evt-$mode.json"
  Set-Content $jf $body -Encoding utf8
  $txt = Dump "evt-create-$mode" @("--session=f4-prof","--print=hb","POST","http://localhost:8080/events","X-XSRF-TOKEN:$cp","@$jf")
  if ($txt -match $script:idRe) { return $Matches[1] }
  return $null
}

$evAgendado = CreateEvent "SECRET_SINGLE" "Fatia4 AGENDADO 409"
Dump "evt-audience" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/events?audience=me") | Out-Null
if ($evAgendado) {
  Dump "sess-agendado" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/events/$evAgendado/attendance/session") | Out-Null
  Dump "entry-agendado-409" @("--session=f4-aluno","--print=hb","POST","http://localhost:8080/events/$evAgendado/attendance/entry","X-XSRF-TOKEN:$ca","pin=000000","deviceUuid=aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeee0001") | Out-Null
}

$i = 0
foreach ($mode in @("SECRET_SINGLE","SECRET_DUAL","QR_SINGLE","QR_DUAL")) {
  $i++
  $eid = CreateEvent $mode ("Fatia4 " + $mode)
  if (-not $eid) { continue }
  Dump "sess-before-$mode" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/events/$eid/attendance/session") | Out-Null
  $win = Dump "win-entry-$mode" @("--session=f4-prof","--print=hb","POST","http://localhost:8080/events/$eid/attendance/windows/entry","X-XSRF-TOKEN:$cp","durationSeconds:=900")
  $pin = $null; $qr = $null
  if ($win -match $secretRe) { $pin = $Matches[1] }
  if ($win -match $qrRe) { $qr = $Matches[1] }
  Dump "sess-open-$mode" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/events/$eid/attendance/session") | Out-Null
  $dev = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeee00{0:d2}" -f $i
  if ($mode.StartsWith("SECRET")) {
    Dump "entry-$mode" @("--session=f4-aluno","--print=hb","POST","http://localhost:8080/events/$eid/attendance/entry","X-XSRF-TOKEN:$ca","pin=$pin","deviceUuid=$dev") | Out-Null
  } else {
    Dump "entry-$mode" @("--session=f4-aluno","--print=hb","POST","http://localhost:8080/events/$eid/attendance/qr/validate","X-XSRF-TOKEN:$ca","qrToken=$qr","deviceUuid=$dev") | Out-Null
  }
  Dump "sess-after-entry-$mode" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/events/$eid/attendance/session") | Out-Null
  if ($mode.EndsWith("DUAL")) {
    $win2 = Dump "win-exit-$mode" @("--session=f4-prof","--print=hb","POST","http://localhost:8080/events/$eid/attendance/windows/exit","X-XSRF-TOKEN:$cp","durationSeconds:=900")
    $pin2 = $null; $qr2 = $null
    if ($win2 -match $secretRe) { $pin2 = $Matches[1] }
    if ($win2 -match $qrRe) { $qr2 = $Matches[1] }
    Dump "sess-exitwin-$mode" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/events/$eid/attendance/session") | Out-Null
    if ($mode.StartsWith("SECRET")) {
      Dump "exit-$mode" @("--session=f4-aluno","--print=hb","POST","http://localhost:8080/events/$eid/attendance/exit","X-XSRF-TOKEN:$ca","pin=$pin2","deviceUuid=$dev") | Out-Null
    } else {
      Dump "exit-$mode" @("--session=f4-aluno","--print=hb","POST","http://localhost:8080/events/$eid/attendance/qr/validate","X-XSRF-TOKEN:$ca","qrToken=$qr2","deviceUuid=$dev") | Out-Null
    }
  }
  Dump "close-$mode" @("--session=f4-prof","--print=hb","POST","http://localhost:8080/events/$eid/close","X-XSRF-TOKEN:$cp") | Out-Null
}

$certs = Dump "certs-mine" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/certificates/mine")
$certId = $null; $hash = $null
if ($certs -match $idRe) { $certId = $Matches[1] }
if ($certs -match $hashRe) { $hash = $Matches[1] }
if ($certId) {
  Dump "cert-download" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/certificates/$certId/download-url") | Out-Null
}
Dump "cert-idor" @("--session=f4-aluno","--print=hb","GET","http://localhost:8080/certificates/00000000-0000-7000-8000-000000000001/download-url") | Out-Null
if ($hash) {
  Dump "cert-verify" @("--print=hb","GET","http://localhost:8080/publico/verificar-certificado/$hash") | Out-Null
}

$results | Format-Table -AutoSize
($results | ConvertTo-Json) | Set-Content (Join-Path $raw "http-battery.json") -Encoding utf8
Write-Host "DONE"

$ErrorActionPreference = "Continue"
$http = "C:\Users\zuria\AppData\Roaming\Python\Python313\Scripts\http.exe"
$raw = "c:\Users\zuria\CODE\TCC\secretariaonline2\secretariaOnline2\logs\raw\fatia-6"
$env:PYTHONUTF8 = "1"
$cursoId = "01a05940-ec36-75cc-ab53-d999ce0a7fa1"
$profId = "98fe1066-4c4c-4f20-b911-2941e0c921a0"
$now = [DateTimeOffset]::UtcNow
$inicio = $now.AddHours(-1).ToString("yyyy-MM-ddTHH:mm:ssZ")
$fim = $now.AddHours(4).ToString("yyyy-MM-ddTHH:mm:ssZ")
$results = New-Object System.Collections.Generic.List[object]
$idRe = '"id"\s*:\s*"([0-9a-fA-F-]{36})"'
$secretRe = '"secret"\s*:\s*"([^"]+)"'

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
Dump "login-prof" @('--session=f6-prof','--print=hb','POST','http://localhost:8080/auth/login','identificador=prof.ana@ufpr.br','senha=ProfS3nh@Forte!') | Out-Null
Dump "login-aluno" @('--session=f6-aluno','--print=hb','POST','http://localhost:8080/auth/login','identificador=ana.aluno@ufpr.br','senha=AlunoS3nh@Forte!') | Out-Null
Dump "login-admin" @('--session=f6-admin','--print=hb','POST','http://localhost:8080/auth/login','identificador=admin@ufpr.br','senha=Admin@123456') | Out-Null

$cp = (& $http --session=f6-prof --print=b GET http://localhost:8080/auth/csrf | ConvertFrom-Json).token
$ca = (& $http --session=f6-aluno --print=b GET http://localhost:8080/auth/csrf | ConvertFrom-Json).token
$cadm = (& $http --session=f6-admin --print=b GET http://localhost:8080/auth/csrf | ConvertFrom-Json).token

Dump "dash-prof" @('--session=f6-prof','--print=hb','GET','http://localhost:8080/bff/dashboard/professor') | Out-Null
Dump "dash-prof-as-aluno" @('--session=f6-aluno','--print=hb','GET','http://localhost:8080/bff/dashboard/professor') | Out-Null
Dump "me-prof" @('--session=f6-prof','--print=hb','GET','http://localhost:8080/me') | Out-Null
Dump "events-host-me" @('--session=f6-prof','--print=hb','GET','http://localhost:8080/events?host=me') | Out-Null

function EventJson([string]$mode) {
  $obj = @{
    titulo = "Fatia6 $mode"
    descricao = "Host fatia 6 janela agora"
    idCurso = $cursoId
    attendanceMode = $mode
    chCreditadas = 4.0
    inicioEm = $inicio
    fimEm = $fim
  } | ConvertTo-Json
  $path = Join-Path $raw "_evt-$mode.json"
  Set-Content $path $obj -Encoding utf8
  return "@$path"
}

$ss = Dump "evt-create-SECRET_SINGLE" @('--session=f6-prof','--print=hb','POST','http://localhost:8080/events',"X-XSRF-TOKEN:$cp",(EventJson 'SECRET_SINGLE'))
$evtSs = $null
if ($ss -match $idRe) { $evtSs = $Matches[1] }
Write-Host "evtSs=$evtSs"

Dump "evt-detail-agendado" @('--session=f6-prof','--print=hb','GET',"http://localhost:8080/events/$evtSs") | Out-Null
Dump "evt-create-aluno-403" @('--session=f6-aluno','--print=hb','POST','http://localhost:8080/events',"X-XSRF-TOKEN:$ca",(EventJson 'SECRET_SINGLE')) | Out-Null

$win = Dump "win-entry-SECRET_SINGLE" @('--session=f6-prof','--print=hb','POST',"http://localhost:8080/events/$evtSs/attendance/windows/entry","X-XSRF-TOKEN:$cp",'durationSeconds:=900')
$pin = $null
if ($win -match $secretRe) { $pin = $Matches[1] }
Write-Host "pin=$pin"
Dump "evt-detail-andamento" @('--session=f6-prof','--print=hb','GET',"http://localhost:8080/events/$evtSs") | Out-Null
Dump "sess-aluno" @('--session=f6-aluno','--print=hb','GET',"http://localhost:8080/events/$evtSs/attendance/session") | Out-Null
Dump "entry-aluno" @('--session=f6-aluno','--print=hb','POST',"http://localhost:8080/events/$evtSs/attendance/entry","X-XSRF-TOKEN:$ca","pin=$pin",'qrToken:=null','deviceUuid=fatia6-device-aluno') | Out-Null
Dump "close-SECRET_SINGLE" @('--session=f6-prof','--print=hb','POST',"http://localhost:8080/events/$evtSs/close","X-XSRF-TOKEN:$cp") | Out-Null
Dump "evt-detail-closed" @('--session=f6-prof','--print=hb','GET',"http://localhost:8080/events/$evtSs") | Out-Null

foreach ($mode in @('SECRET_DUAL','QR_SINGLE','QR_DUAL')) {
  $created = Dump "evt-create-$mode" @('--session=f6-prof','--print=hb','POST','http://localhost:8080/events',"X-XSRF-TOKEN:$cp",(EventJson $mode))
  $eid = $null
  if ($created -match $idRe) { $eid = $Matches[1] }
  Write-Host "created $mode $eid"
  Dump "win-entry-$mode" @('--session=f6-prof','--print=hb','POST',"http://localhost:8080/events/$eid/attendance/windows/entry","X-XSRF-TOKEN:$cp",'durationSeconds:=900') | Out-Null
}

Dump "wrong-path-caaf-dashboard" @('--session=f6-prof','--print=hb','GET','http://localhost:8080/commissions/caaf/dashboard') | Out-Null

$form = @{
  titulo = "Fatia6 CAAF pendente"
  descricao = "Para pool CAAF"
  categoria = "PALESTRA"
  cargaHoraria = 4.0
  dataRealizacao = "2026-06-20"
} | ConvertTo-Json
$formPath = Join-Path $raw "_formativa.json"
Set-Content $formPath $form -Encoding utf8
$fp = Dump "formativa-post" @('--session=f6-aluno','--print=hb','POST','http://localhost:8080/formativas',"X-XSRF-TOKEN:$ca","@$formPath")
$formId = $null
if ($fp -match $idRe) { $formId = $Matches[1] }
Write-Host "formId=$formId"

Dump "caaf-pool-prof" @('--session=f6-prof','--print=hb','GET','http://localhost:8080/commissions/caaf/pool?page=0&size=20') | Out-Null
Dump "caaf-pool-aluno-403" @('--session=f6-aluno','--print=hb','GET','http://localhost:8080/commissions/caaf/pool') | Out-Null
Dump "caaf-claim" @('--session=f6-prof','--print=hb','POST',"http://localhost:8080/commissions/caaf/$formId/claim","X-XSRF-TOKEN:$cp") | Out-Null
Dump "caaf-claim-again" @('--session=f6-prof','--print=hb','POST',"http://localhost:8080/commissions/caaf/$formId/claim","X-XSRF-TOKEN:$cp") | Out-Null

$batch = @{
  ids = @($formId)
  acao = "APROVAR"
  parecer = "Lote CAAF fatia 6"
} | ConvertTo-Json
$batchPath = Join-Path $raw "_batch.json"
Set-Content $batchPath $batch -Encoding utf8
Dump "caaf-batch" @('--session=f6-prof','--print=hb','POST','http://localhost:8080/commissions/caaf/batch-review',"X-XSRF-TOKEN:$cp","@$batchPath") | Out-Null
Dump "caaf-stats" @('--session=f6-prof','--print=hb','GET','http://localhost:8080/commissions/caaf/stats') | Out-Null

Dump "coe-pool-prof-403" @('--session=f6-prof','--print=hb','GET','http://localhost:8080/commissions/coe/pool') | Out-Null
Dump "coe-pool-aluno-403" @('--session=f6-aluno','--print=hb','GET','http://localhost:8080/commissions/coe/pool') | Out-Null

$intern = Dump "internship-post" @('--session=f6-aluno','--print=hb','POST','http://localhost:8080/internships',"X-XSRF-TOKEN:$ca",'empresa=Empresa COE Fatia6','cargo=Dev','cargaHorariaSemanal:=20','inicio=2026-03-01','observacoes=pool COE')
$internId = $null
if ($intern -match $idRe) { $internId = $Matches[1] }
Write-Host "internId=$internId"

Dump "coe-pool-admin" @('--session=f6-admin','--print=hb','GET','http://localhost:8080/commissions/coe/pool?page=0&size=20') | Out-Null
Dump "usuarios-email-admin" @('--session=f6-admin','--print=hb','GET','http://localhost:8080/usuarios?email=prof.ana@ufpr.br') | Out-Null
Dump "usuarios-email-prof-403" @('--session=f6-prof','--print=hb','GET','http://localhost:8080/usuarios?email=prof.ana@ufpr.br') | Out-Null
Dump "coe-assign" @('--session=f6-admin','--print=hb','POST',"http://localhost:8080/commissions/coe/$internId/assign-supervisor","X-XSRF-TOKEN:$cadm","idSupervisor=$profId") | Out-Null
Dump "coe-stats" @('--session=f6-admin','--print=hb','GET','http://localhost:8080/commissions/coe/stats') | Out-Null

$comm = @{
  titulo = "Aviso da turma TADS fatia 6"
  conteudo = "Prazo de formativas encerra sexta-feira as 18h."
  tipo = "AVISO"
  cursoId = $cursoId
} | ConvertTo-Json
$commPath = Join-Path $raw "_comm.json"
Set-Content $commPath $comm -Encoding utf8
Dump "comm-publish" @('--session=f6-prof','--print=hb','POST','http://localhost:8080/communications',"X-XSRF-TOKEN:$cp","@$commPath") | Out-Null

$commNoCurso = @{
  titulo = "Aviso sem curso"
  conteudo = "deve falhar publish_class"
  tipo = "AVISO"
} | ConvertTo-Json
$commNoPath = Join-Path $raw "_comm-nocurso.json"
Set-Content $commNoPath $commNoCurso -Encoding utf8
Dump "comm-publish-nocurso" @('--session=f6-prof','--print=hb','POST','http://localhost:8080/communications',"X-XSRF-TOKEN:$cp","@$commNoPath") | Out-Null
Dump "comm-publish-aluno-403" @('--session=f6-aluno','--print=hb','POST','http://localhost:8080/communications',"X-XSRF-TOKEN:$ca","@$commPath") | Out-Null
Dump "inbox-aluno" @('--session=f6-aluno','--print=hb','GET','http://localhost:8080/communications/me?page=0&size=5') | Out-Null

$results | Format-Table -AutoSize
$results | ConvertTo-Json | Set-Content (Join-Path $raw "http-battery.json") -Encoding utf8
Write-Host "evtSs=$evtSs formId=$formId internId=$internId pin=$pin"

$ErrorActionPreference = "Continue"
$http = "C:\Users\zuria\AppData\Roaming\Python\Python313\Scripts\http.exe"
$raw = "c:\Users\zuria\CODE\TCC\secretariaonline2\secretariaOnline2\logs\raw\fatia-7"
New-Item -ItemType Directory -Force -Path $raw | Out-Null
$env:PYTHONUTF8 = "1"
$cursoId = "01a05940-ec36-75cc-ab53-d999ce0a7fa1"
$alunoId = "1bafbb82-a473-4170-8433-c13cebc22562"
$results = New-Object System.Collections.Generic.List[object]
$idRe = '"id"\s*:\s*"([0-9a-fA-F-]{36})"'

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
Dump "login-sec" @('--session=f7-sec','--print=hb','POST','http://localhost:8080/auth/login','identificador=secretaria@ufpr.br','senha=SecrS3nh@Forte!') | Out-Null
Dump "login-coord" @('--session=f7-coord','--print=hb','POST','http://localhost:8080/auth/login','identificador=coord.tads@ufpr.br','senha=CoordS3nh@Forte!') | Out-Null
Dump "login-admin" @('--session=f7-admin','--print=hb','POST','http://localhost:8080/auth/login','identificador=admin@ufpr.br','senha=Admin@123456') | Out-Null
Dump "login-aluno" @('--session=f7-aluno','--print=hb','POST','http://localhost:8080/auth/login','identificador=ana.aluno@ufpr.br','senha=AlunoS3nh@Forte!') | Out-Null
Dump "login-prof" @('--session=f7-prof','--print=hb','POST','http://localhost:8080/auth/login','identificador=prof.ana@ufpr.br','senha=ProfS3nh@Forte!') | Out-Null

$cs = (& $http --session=f7-sec --print=b GET http://localhost:8080/auth/csrf | ConvertFrom-Json).token
$cc = (& $http --session=f7-coord --print=b GET http://localhost:8080/auth/csrf | ConvertFrom-Json).token
$cadm = (& $http --session=f7-admin --print=b GET http://localhost:8080/auth/csrf | ConvertFrom-Json).token
$ca = (& $http --session=f7-aluno --print=b GET http://localhost:8080/auth/csrf | ConvertFrom-Json).token

Dump "dash-sec" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/bff/dashboard/secretaria') | Out-Null
Dump "dash-sec-as-prof" @('--session=f7-prof','--print=hb','GET','http://localhost:8080/bff/dashboard/secretaria') | Out-Null
Dump "usuarios-email-sec" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/usuarios?email=ana.aluno@ufpr.br') | Out-Null
Dump "usuarios-email-aluno-403" @('--session=f7-aluno','--print=hb','GET','http://localhost:8080/usuarios?email=ana.aluno@ufpr.br') | Out-Null

$typesOut = Dump "requests-types-sec" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/requests/types')
$typeId = $null
if ($typesOut -match '"code"\s*:\s*"DECLARACAO_MATRICULA"[\s\S]{0,200}?"id"\s*:\s*"([0-9a-fA-F-]{36})"') {
  $typeId = $Matches[1]
} elseif ($typesOut -match '"id"\s*:\s*"([0-9a-fA-F-]{36})"[\s\S]{0,200}?"code"\s*:\s*"DECLARACAO_MATRICULA"') {
  $typeId = $Matches[1]
}
Write-Host "typeId=$typeId"

$ob = @{
  idRequestType = $typeId
  idCurso = $cursoId
  idSolicitanteOnBehalf = $alunoId
  dados = @{ finalidade = "BOLSA"; observacoes = "Aberta pelo balcao fatia 7" }
} | ConvertTo-Json
Set-Content (Join-Path $raw "_onbehalf.json") $ob -Encoding utf8
$obOut = Dump "onbehalf-post" @('--session=f7-sec','--print=hb','POST','http://localhost:8080/requests',"X-XSRF-TOKEN:$cs","@$(Join-Path $raw '_onbehalf.json')")
$reqId = $null
if ($obOut -match $idRe) { $reqId = $Matches[1] }
Write-Host "reqId=$reqId"
Dump "onbehalf-detail" @('--session=f7-sec','--print=hb','GET',"http://localhost:8080/requests/$reqId") | Out-Null

$bulk = @{ ids = @($reqId); action = "DEFER"; parecer = "lote ABERTA deve 422" } | ConvertTo-Json
Set-Content (Join-Path $raw "_bulk.json") $bulk -Encoding utf8
Dump "bulk-defer-aberta" @('--session=f7-sec','--print=hb','PATCH','http://localhost:8080/requests/bulk-deliberate',"X-XSRF-TOKEN:$cs","@$(Join-Path $raw '_bulk.json')") | Out-Null

$assign = @{ action = "ASSIGN"; parecer = "triagem fatia 7" } | ConvertTo-Json
Set-Content (Join-Path $raw "_assign.json") $assign -Encoding utf8
Dump "assign-then" @('--session=f7-sec','--print=hb','POST',"http://localhost:8080/requests/$reqId/transitions","X-XSRF-TOKEN:$cs","@$(Join-Path $raw '_assign.json')") | Out-Null
$defer = @{ action = "DEFER"; parecer = "ok apos ASSIGN" } | ConvertTo-Json
Set-Content (Join-Path $raw "_defer.json") $defer -Encoding utf8
Dump "defer-after-assign" @('--session=f7-sec','--print=hb','POST',"http://localhost:8080/requests/$reqId/transitions","X-XSRF-TOKEN:$cs","@$(Join-Path $raw '_defer.json')") | Out-Null

Dump "search-aluno" @('--session=f7-aluno','--print=hb','GET','http://localhost:8080/search?q=ana&page=0&size=10') | Out-Null
Dump "search-sec-usuario" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/search?q=ana&types=USUARIO') | Out-Null
Dump "search-admin" @('--session=f7-admin','--print=hb','GET','http://localhost:8080/search?q=ana&page=0&size=10') | Out-Null

Dump "tasks-sec" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/tasks') | Out-Null
Dump "tarefas-wrong" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/tarefas') | Out-Null
$task = @{ titulo = "Fatia7 kanban"; prioridade = "ALTA"; prazoEm = "2026-09-25T17:00:00Z" } | ConvertTo-Json
Set-Content (Join-Path $raw "_task.json") $task -Encoding utf8
$taskOut = Dump "task-create" @('--session=f7-sec','--print=hb','POST','http://localhost:8080/tasks',"X-XSRF-TOKEN:$cs","@$(Join-Path $raw '_task.json')")
$taskId = $null
if ($taskOut -match $idRe) { $taskId = $Matches[1] }
Write-Host "taskId=$taskId"

Dump "reports-sec" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/reports/secretary?periodo=2026-2&curso=TADS') | Out-Null
Dump "reports-coord" @('--session=f7-coord','--print=hb','GET','http://localhost:8080/reports/coordinator?periodo=2026-2&curso=TADS') | Out-Null
Dump "course-config-coord" @('--session=f7-coord','--print=hb','GET','http://localhost:8080/courses/tads/config') | Out-Null
Dump "course-config-aluno-403" @('--session=f7-aluno','--print=hb','GET','http://localhost:8080/courses/tads/config') | Out-Null

Dump "request-types-admin" @('--session=f7-admin','--print=hb','GET','http://localhost:8080/request-types') | Out-Null
Dump "request-types-aluno-403" @('--session=f7-aluno','--print=hb','GET','http://localhost:8080/request-types') | Out-Null

$code = "FATIA7_" + ([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())
$rt = @{
  code = $code
  descricao = "Tipo rascunho fatia 7 HTTP"
  prazoDias = 5
  formSchema = @{ type = "object"; properties = @{ finalidade = @{ type = "string"; enum = @("BOLSA","OUTRO") } }; required = @("finalidade") }
  workflowJson = @{
    initial = "ABERTA"
    states = @("RASCUNHO","ABERTA","EM_TRIAGEM","DEFERIDA","ARQUIVADA")
    transitions = @(
      @{ from = "ABERTA"; to = "EM_TRIAGEM"; action = "ASSIGN"; requiresAuthority = @("request.deliberate") }
      @{ from = "EM_TRIAGEM"; to = "DEFERIDA"; action = "DEFER"; requiresAuthority = @("request.deliberate") }
    )
  }
} | ConvertTo-Json -Depth 8
Set-Content (Join-Path $raw "_rt.json") $rt -Encoding utf8
$rtOut = Dump "rt-create" @('--session=f7-admin','--print=hb','POST','http://localhost:8080/request-types',"X-XSRF-TOKEN:$cadm","@$(Join-Path $raw '_rt.json')")
$rtId = $null
if ($rtOut -match $idRe) { $rtId = $Matches[1] }
Write-Host "rtId=$rtId code=$code"
Dump "rt-publish" @('--session=f7-admin','--print=hb','POST',"http://localhost:8080/request-types/$rtId/publish","X-XSRF-TOKEN:$cadm") | Out-Null
Dump "requests-types-aluno-after" @('--session=f7-aluno','--print=hb','GET','http://localhost:8080/requests/types') | Out-Null

Dump "outbox-pending" @('--session=f7-admin','--print=hb','GET','http://localhost:8080/admin/outbox') | Out-Null
Dump "outbox-processed" @('--session=f7-admin','--print=hb','GET','http://localhost:8080/admin/outbox?status=PROCESSED') | Out-Null
Dump "admin-roles" @('--session=f7-admin','--print=hb','GET','http://localhost:8080/admin/roles') | Out-Null
Dump "admin-autoridades" @('--session=f7-admin','--print=hb','GET','http://localhost:8080/admin/autoridades') | Out-Null
Dump "admin-audit" @('--session=f7-admin','--print=hb','GET','http://localhost:8080/admin/audit?page=0&size=5') | Out-Null
Dump "templates" @('--session=f7-admin','--print=hb','GET','http://localhost:8080/communication-templates') | Out-Null
Dump "graduations" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/graduations') | Out-Null
Dump "students-elig" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/students?eligibleForGraduation=true') | Out-Null
Dump "usuarios-patch-wrong" @('--session=f7-sec','--print=hb','PATCH',"http://localhost:8080/usuarios/$alunoId","X-XSRF-TOKEN:$cs",'ativo:=false') | Out-Null
Dump "usuarios-status" @('--session=f7-sec','--print=hb','PATCH',"http://localhost:8080/usuarios/$alunoId/status","X-XSRF-TOKEN:$cs",'ativo:=true') | Out-Null

Dump "export-alunos" @('--session=f7-sec','--print=hb','POST','http://localhost:8080/exports/alunos',"X-XSRF-TOKEN:$cs") | Out-Null
$csv = "nome,email,grr,role`nFatia7 Import,fatia7.import@ufpr.br,20219999,ALUNO`n"
$csvPath = Join-Path $raw "_import.csv"
Set-Content $csvPath $csv -Encoding utf8
Dump "import-alunos" @('--session=f7-sec','--print=hb','--form','POST','http://localhost:8080/imports/alunos',"X-XSRF-TOKEN:$cs","file@$csvPath") | Out-Null

$sr = @{ idAluno = $alunoId; assunto = "Balcao fatia 7"; tipo = "PRESENCIAL"; descricao = "HTTP" } | ConvertTo-Json
Set-Content (Join-Path $raw "_sr.json") $sr -Encoding utf8
Dump "service-record" @('--session=f7-sec','--print=hb','POST','http://localhost:8080/service-records',"X-XSRF-TOKEN:$cs","@$(Join-Path $raw '_sr.json')") | Out-Null
Dump "tickets-staff" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/support/tickets') | Out-Null
Dump "tickets-aluno-403" @('--session=f7-aluno','--print=hb','GET','http://localhost:8080/support/tickets') | Out-Null
Dump "faq" @('--session=f7-admin','--print=hb','GET','http://localhost:8080/faq') | Out-Null
Dump "disciplinas-search" @('--session=f7-sec','--print=hb','GET','http://localhost:8080/academico/disciplinas?search=cal') | Out-Null

$bat = $results | ConvertTo-Json
Set-Content (Join-Path $raw "http-battery.json") $bat -Encoding utf8
Write-Host ""
Write-Host "BATTERY"
$results | Format-Table -AutoSize
Write-Host "reqId=$reqId rtId=$rtId code=$code taskId=$taskId"

$baseUrl = "http://localhost:8082/api"
$Headers = @{
    "Content-Type" = "application/json"
}

Write-Host "--- TEST 1: Login as Admin ---"
$loginBody = @{
    email = "admin@gmail.com"
    password = "admin"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Headers $Headers -Body $loginBody
Write-Host "Login Response: $($loginRes | ConvertTo-Json -Depth 3 -Compress)"

$token = $loginRes.token
$AuthHeaders = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}

Write-Host "`n--- TEST 2: Create Department ---"
$deptBody = @{
    name = "Engineering " + (Get-Random)
} | ConvertTo-Json
$deptRes = Invoke-RestMethod -Uri "$baseUrl/admin/departments" -Method Post -Headers $AuthHeaders -Body $deptBody
Write-Host "Dept Created: $($deptRes | ConvertTo-Json -Depth 3 -Compress)"
$deptId = $deptRes.id

Write-Host "`n--- TEST 3: Create Team ---"
$teamBody = @{
    name = "Backend Wizards " + (Get-Random)
    departmentId = $deptId
} | ConvertTo-Json
$teamRes = Invoke-RestMethod -Uri "$baseUrl/admin/teams" -Method Post -Headers $AuthHeaders -Body $teamBody
Write-Host "Team Created: $($teamRes | ConvertTo-Json -Depth 3 -Compress)"
$teamId = $teamRes.id

Write-Host "`n--- TEST 4: Create Employee (with validation check) ---"
$empBody = @{
    name = "Test Employee " + (Get-Random)
    email = "testemp$((Get-Random))@company.com"
    password = "password123"
    departmentId = $deptId
    teamId = $teamId
} | ConvertTo-Json
$empRes = Invoke-RestMethod -Uri "$baseUrl/admin/employees" -Method Post -Headers $AuthHeaders -Body $empBody
Write-Host "Employee Created: $($empRes | ConvertTo-Json -Depth 3 -Compress)"
$empId = $empRes.id

Write-Host "`n--- TEST 5: Create Task ---"
$taskBody = @{
    title = "Setup Database"
    description = "Install MongoDB and set up replica set"
    priority = "HIGH"
    status = "ASSIGNED"
} | ConvertTo-Json
$taskRes = Invoke-RestMethod -Uri "$baseUrl/admin/tasks" -Method Post -Headers $AuthHeaders -Body $taskBody
Write-Host "Task Created: $($taskRes | ConvertTo-Json -Depth 3 -Compress)"
$taskId = $taskRes.id

Write-Host "`n--- TEST 6: Assign Task ---"
$assignRes = Invoke-RestMethod -Uri "$baseUrl/admin/tasks/$taskId/assign?employeeId=$empId" -Method Post -Headers $AuthHeaders
Write-Host "Task Assigned: $($assignRes | ConvertTo-Json -Depth 3 -Compress)"

Write-Host "`n--- TEST 7: Get Dashboard Stats ---"
$dashRes = Invoke-RestMethod -Uri "$baseUrl/admin/dashboard" -Method Get -Headers $AuthHeaders
Write-Host "Dashboard Stats: $($dashRes | ConvertTo-Json -Depth 3 -Compress)"

Write-Host "`n--- TEST 8: Pagination Get Employees ---"
$empPageRes = Invoke-RestMethod -Uri "$baseUrl/admin/employees?page=0&size=5" -Method Get -Headers $AuthHeaders
Write-Host "Employee Pagination (Total): $($empPageRes.totalElements)"
Write-Host "Employee Pagination (Content size): $($empPageRes.content.Count)"

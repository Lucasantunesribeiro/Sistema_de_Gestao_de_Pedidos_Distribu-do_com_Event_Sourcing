param(
    [string]$BaseUrl = "http://localhost:8084",
    [int]$TimeoutSeconds = 30
)

Write-Host "=== Order Management System Integration Tests ===" -ForegroundColor Blue

function Write-Status {
    param([string]$Message)
    Write-Host "[TEST] $Message" -ForegroundColor Green
}

function Write-Success {
    param([string]$Message)
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Write-Failure {
    param([string]$Message)
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

$testResults = @()

# Test 1: Health Check
Write-Status "Testing health endpoints..."
try {
    $healthResponse = Invoke-RestMethod -Uri "$BaseUrl/api/orders/health" -TimeoutSec $TimeoutSeconds
    if ($healthResponse.status -eq "healthy") {
        Write-Success "Health check passed"
        $testResults += @{Test="Health Check"; Status="PASS"}
    } else {
        Write-Failure "Health check failed - status: $($healthResponse.status)"
        $testResults += @{Test="Health Check"; Status="FAIL"}
    }
} catch {
    Write-Failure "Health check failed - error: $($_.Exception.Message)"
    $testResults += @{Test="Health Check"; Status="FAIL"}
}

# Test 2: Get all orders
Write-Status "Testing GET /api/orders..."
try {
    $ordersResponse = Invoke-RestMethod -Uri "$BaseUrl/api/orders" -TimeoutSec $TimeoutSeconds
    if ($ordersResponse.success -eq $true) {
        Write-Success "GET /api/orders returned success with $($ordersResponse.count) orders"
        $testResults += @{Test="GET Orders"; Status="PASS"}
    } else {
        Write-Failure "GET /api/orders did not return success"
        $testResults += @{Test="GET Orders"; Status="FAIL"}
    }
} catch {
    Write-Failure "GET /api/orders failed - error: $($_.Exception.Message)"
    $testResults += @{Test="GET Orders"; Status="FAIL"}
}

# Test 3: Get orders by customer ID
Write-Status "Testing GET /api/orders/customer/1..."
try {
    $customerOrdersResponse = Invoke-RestMethod -Uri "$BaseUrl/api/orders/customer/1" -TimeoutSec $TimeoutSeconds
    if ($customerOrdersResponse.success -eq $true) {
        Write-Success "GET /api/orders/customer/1 returned success with $($customerOrdersResponse.count) orders"
        $testResults += @{Test="GET Orders by Customer"; Status="PASS"}
    } else {
        Write-Failure "GET /api/orders/customer/1 did not return success"
        $testResults += @{Test="GET Orders by Customer"; Status="FAIL"}
    }
} catch {
    Write-Failure "GET /api/orders/customer/1 failed - error: $($_.Exception.Message)"
    $testResults += @{Test="GET Orders by Customer"; Status="FAIL"}
}

# Test 4: Get orders by status
Write-Status "Testing GET /api/orders/status/PENDING..."
try {
    $statusOrdersResponse = Invoke-RestMethod -Uri "$BaseUrl/api/orders/status/PENDING" -TimeoutSec $TimeoutSeconds
    if ($statusOrdersResponse.success -eq $true) {
        Write-Success "GET /api/orders/status/PENDING returned success with $($statusOrdersResponse.count) orders"
        $testResults += @{Test="GET Orders by Status"; Status="PASS"}
    } else {
        Write-Failure "GET /api/orders/status/PENDING did not return success"
        $testResults += @{Test="GET Orders by Status"; Status="FAIL"}
    }
} catch {
    Write-Failure "GET /api/orders/status/PENDING failed - error: $($_.Exception.Message)"
    $testResults += @{Test="GET Orders by Status"; Status="FAIL"}
}

# Test 5: Dashboard metrics
Write-Status "Testing GET /api/orders/dashboard/metrics..."
try {
    $metricsResponse = Invoke-RestMethod -Uri "$BaseUrl/api/orders/dashboard/metrics" -TimeoutSec $TimeoutSeconds
    if ($metricsResponse.success -eq $true -and $metricsResponse.metrics) {
        Write-Success "GET /api/orders/dashboard/metrics returned success with metrics"
        $testResults += @{Test="Dashboard Metrics"; Status="PASS"}
    } else {
        Write-Failure "GET /api/orders/dashboard/metrics did not return valid metrics"
        $testResults += @{Test="Dashboard Metrics"; Status="FAIL"}
    }
} catch {
    Write-Failure "GET /api/orders/dashboard/metrics failed - error: $($_.Exception.Message)"
    $testResults += @{Test="Dashboard Metrics"; Status="FAIL"}
}

# Test 6: CQRS Demo
Write-Status "Testing GET /api/orders/cqrs/demo..."
try {
    $cqrsResponse = Invoke-RestMethod -Uri "$BaseUrl/api/orders/cqrs/demo" -TimeoutSec $TimeoutSeconds
    if ($cqrsResponse.success -eq $true -and $cqrsResponse.explanation) {
        Write-Success "GET /api/orders/cqrs/demo returned success with explanation"
        $testResults += @{Test="CQRS Demo"; Status="PASS"}
    } else {
        Write-Failure "GET /api/orders/cqrs/demo did not return valid demo"
        $testResults += @{Test="CQRS Demo"; Status="FAIL"}
    }
} catch {
    Write-Failure "GET /api/orders/cqrs/demo failed - error: $($_.Exception.Message)"
    $testResults += @{Test="CQRS Demo"; Status="FAIL"}
}

# Test 7: Non-existent order
Write-Status "Testing GET /api/orders/99999 (non-existent)..."
try {
    $nonExistentResponse = Invoke-RestMethod -Uri "$BaseUrl/api/orders/99999" -TimeoutSec $TimeoutSeconds
    Write-Failure "GET /api/orders/99999 should have returned 404 but succeeded"
    $testResults += @{Test="GET Non-existent Order"; Status="FAIL"}
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Success "GET /api/orders/99999 correctly returned 404"
        $testResults += @{Test="GET Non-existent Order"; Status="PASS"}
    } else {
        Write-Failure "GET /api/orders/99999 failed with unexpected error: $($_.Exception.Message)"
        $testResults += @{Test="GET Non-existent Order"; Status="FAIL"}
    }
}

# Summary
Write-Host ""
Write-Host "=== Test Results Summary ===" -ForegroundColor Blue
$passCount = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $testResults.Count

foreach ($result in $testResults) {
    if ($result.Status -eq "PASS") {
        Write-Host "OK $($result.Test)" -ForegroundColor Green
    } else {
        Write-Host "FAIL $($result.Test)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Total Tests: $totalCount" -ForegroundColor Blue
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor Red

if ($failCount -eq 0) {
    Write-Host ""
    Write-Host "All tests passed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host ""
    Write-Host "Some tests failed. Please check the system." -ForegroundColor Red
    exit 1
}
$srcDir = "D:\Backend\dekiru\src\main\java\ken\example\dekiru"
Write-Host "Reorganizing directories..."
Rename-Item -Path "$srcDir\common\ApiReponse" -NewName "response" -ErrorAction SilentlyContinue
Rename-Item -Path "$srcDir\common\Configuration" -NewName "config" -ErrorAction SilentlyContinue
Rename-Item -Path "$srcDir\common\Exception" -NewName "exception" -ErrorAction SilentlyContinue
Move-Item -Path "$srcDir\specification" -Destination "$srcDir\common\specification" -ErrorAction SilentlyContinue
$domains = @("academic", "schedule", "security", "student", "attendance", "dashboard")
foreach ($d in $domains) {
    $layers = @("controller", "service", "repository", "entity", "dto", "mapper")
    foreach ($l in $layers) {
        $path = "$srcDir\$d\$l"
        if (!(Test-Path -Path $path)) { New-Item -ItemType Directory -Path $path | Out-Null }
    }
}
if (!(Test-Path -Path "$srcDir\security\jwt")) { New-Item -ItemType Directory -Path "$srcDir\security\jwt" | Out-Null }
Write-Host "Folders updated."

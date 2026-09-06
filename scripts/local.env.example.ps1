# Copy this file to local.env.ps1 and adjust values for your machine.
# local.env.ps1 is ignored by Git because it contains local credentials.
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "123456"
$env:TESTPILOT_ENCRYPTION_KEY = "local-dev-encryption-key-change-this"

# Optional overrides:
# $env:DB_URL = "jdbc:mysql://127.0.0.1:3306/testpilot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
# $env:REDIS_HOST = "127.0.0.1"
# $env:REDIS_PORT = "6379"

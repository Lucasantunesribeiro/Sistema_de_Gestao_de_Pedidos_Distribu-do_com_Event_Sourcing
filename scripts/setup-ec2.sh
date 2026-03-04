#!/usr/bin/env bash
# One-time setup for an Amazon Linux 2023 / Ubuntu EC2 instance.
# Run as root (or with sudo): sudo bash scripts/setup-ec2.sh <github-repo-url>
#
# Usage:
#   sudo bash setup-ec2.sh https://github.com/your-org/your-repo.git
#
# After this script finishes:
#   1. Edit /opt/ordersystem/.env and set real secrets
#   2. Run: cd /opt/ordersystem && docker compose up -d
set -euo pipefail

REPO_URL="${1:-}"
INSTALL_DIR="/opt/ordersystem"

if [ -z "$REPO_URL" ]; then
  echo "Usage: sudo bash $0 <github-repo-url>"
  exit 1
fi

detect_os() {
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    echo "$ID"
  fi
}

OS=$(detect_os)
echo "=== Detected OS: $OS ==="

# ── Install Docker ────────────────────────────────────────────────────────────
install_docker_amazon() {
  dnf update -y
  dnf install -y docker git curl
  systemctl enable --now docker
  usermod -aG docker ec2-user
}

install_docker_ubuntu() {
  apt-get update -y
  apt-get install -y ca-certificates curl gnupg lsb-release git
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
  systemctl enable --now docker
  usermod -aG docker ubuntu
}

echo "=== Installing Docker ==="
case "$OS" in
  amzn)  install_docker_amazon ;;
  ubuntu) install_docker_ubuntu ;;
  *)
    echo "Unsupported OS: $OS. Install Docker manually then re-run."
    exit 1
    ;;
esac

# Verify Docker Compose v2
docker compose version

# ── Clone repository ──────────────────────────────────────────────────────────
echo "=== Cloning repository to $INSTALL_DIR ==="
if [ -d "$INSTALL_DIR/.git" ]; then
  echo "Repository already exists, pulling latest..."
  git -C "$INSTALL_DIR" pull origin main
else
  git clone "$REPO_URL" "$INSTALL_DIR"
fi

cd "$INSTALL_DIR"

# ── Environment file ──────────────────────────────────────────────────────────
if [ ! -f .env ]; then
  cp .env.example .env
  echo ""
  echo "============================================================"
  echo " IMPORTANT: Edit $INSTALL_DIR/.env before starting the app"
  echo "   - Set POSTGRES_PASSWORD to a strong password"
  echo "   - Set SPRING_RABBITMQ_PASSWORD to a strong password"
  echo "   - Generate JWT_SECRET_KEY: bash scripts/generate-jwt-secret.sh"
  echo "   - Set SECURITY_SECRET to same value as JWT_SECRET_KEY"
  echo "   - Set SECURITY_ENFORCE_AUTH=true for production"
  echo "============================================================"
fi

# ── Permissions ───────────────────────────────────────────────────────────────
DEPLOY_USER="${SUDO_USER:-ec2-user}"
chown -R "$DEPLOY_USER:$DEPLOY_USER" "$INSTALL_DIR" 2>/dev/null || true

# ── Systemd service (optional) ────────────────────────────────────────────────
cat > /etc/systemd/system/ordersystem.service << EOF
[Unit]
Description=Order System (Docker Compose)
After=docker.service network.target
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$INSTALL_DIR
ExecStart=docker compose up -d
ExecStop=docker compose down
TimeoutStartSec=300
User=$DEPLOY_USER

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable ordersystem

echo ""
echo "=== Setup complete ==="
echo "Next steps:"
echo "  1. Edit $INSTALL_DIR/.env with real secrets"
echo "  2. Start: systemctl start ordersystem"
echo "     (or manually: cd $INSTALL_DIR && docker compose up -d)"
echo "  3. Health check: curl http://localhost:8080/actuator/health"

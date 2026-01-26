#!/bin/bash
#
# Servantin VM Setup Script
# This script sets up an Ubuntu VM with all dependencies for running Servantin
#
# Usage: sudo ./setup-vm.sh
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   log_error "This script must be run as root (use sudo)"
   exit 1
fi

log_info "Starting Servantin VM setup..."

# Update system packages
log_info "Updating system packages..."
apt-get update
apt-get upgrade -y

# Install essential packages
log_info "Installing essential packages..."
apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    software-properties-common \
    git \
    htop \
    vim \
    nano \
    wget \
    unzip \
    jq \
    tree \
    ncdu \
    net-tools \
    dnsutils \
    fail2ban \
    ufw \
    logrotate \
    cron

# Install Docker
log_info "Installing Docker..."
if ! command -v docker &> /dev/null; then
    # Add Docker's official GPG key
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc

    # Add Docker repository
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      tee /etc/apt/sources.list.d/docker.list > /dev/null

    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    log_info "Docker installed successfully"
else
    log_info "Docker is already installed"
fi

# Start and enable Docker
systemctl start docker
systemctl enable docker

# Add current user to docker group (if not root)
if [ -n "$SUDO_USER" ]; then
    usermod -aG docker $SUDO_USER
    log_info "Added $SUDO_USER to docker group"
fi

# Install Docker Compose (standalone, for compatibility)
log_info "Installing Docker Compose standalone..."
DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | jq -r '.tag_name')
curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
log_info "Docker Compose ${DOCKER_COMPOSE_VERSION} installed"

# Setup UFW firewall
log_info "Configuring UFW firewall..."
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow http
ufw allow https
ufw allow 8080/tcp comment 'Backend API'
ufw allow 3000/tcp comment 'Frontend Dev'
ufw --force enable
log_info "UFW firewall configured"

# Configure fail2ban
log_info "Configuring fail2ban..."
cat > /etc/fail2ban/jail.local << 'EOF'
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = ssh
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
EOF

systemctl restart fail2ban
systemctl enable fail2ban
log_info "fail2ban configured"

# Create app directory structure
log_info "Creating application directory structure..."
APP_DIR="/opt/servantin"
mkdir -p $APP_DIR/{data,logs,backups,certs}
chmod 755 $APP_DIR

# Setup log rotation for app logs
cat > /etc/logrotate.d/servantin << 'EOF'
/opt/servantin/logs/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 root root
    sharedscripts
    postrotate
        docker-compose -f /opt/servantin/docker-compose.yml exec -T backend kill -USR1 1 2>/dev/null || true
    endscript
}
EOF

# Setup automatic security updates
log_info "Configuring automatic security updates..."
apt-get install -y unattended-upgrades
cat > /etc/apt/apt.conf.d/20auto-upgrades << 'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
APT::Periodic::AutocleanInterval "7";
EOF

# Setup swap if not present (useful for small VMs)
log_info "Checking swap configuration..."
if [ ! -f /swapfile ]; then
    log_info "Creating swap file..."
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    log_info "2GB swap file created"
else
    log_info "Swap already configured"
fi

# Optimize system settings for containers
log_info "Optimizing system settings..."
cat > /etc/sysctl.d/99-servantin.conf << 'EOF'
# Increase max connections
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535

# Increase file descriptors
fs.file-max = 65535

# VM memory optimization
vm.swappiness = 10
vm.dirty_ratio = 60
vm.dirty_background_ratio = 2

# Network performance
net.ipv4.tcp_fin_timeout = 30
net.ipv4.tcp_keepalive_time = 1200
net.core.netdev_max_backlog = 65535
EOF

sysctl -p /etc/sysctl.d/99-servantin.conf

# Create convenience aliases
log_info "Creating convenience aliases..."
cat >> /etc/bash.bashrc << 'EOF'

# Servantin aliases
alias dc='docker-compose'
alias dcup='docker-compose up -d'
alias dcdown='docker-compose down'
alias dclogs='docker-compose logs -f'
alias dcps='docker-compose ps'
alias dcrestart='docker-compose restart'
alias dcbuild='docker-compose build --no-cache'

# Utility aliases
alias ll='ls -alF'
alias la='ls -A'
alias l='ls -CF'
alias ports='netstat -tulanp'
alias meminfo='free -h'
alias cpuinfo='lscpu'
alias diskinfo='df -h'

# Navigation
alias cdapp='cd /opt/servantin'
EOF

# Print summary
echo ""
log_info "============================================"
log_info "  VM Setup Complete!"
log_info "============================================"
echo ""
log_info "Installed software:"
echo "  - Docker: $(docker --version)"
echo "  - Docker Compose: $(docker-compose --version)"
echo "  - Git: $(git --version)"
echo ""
log_info "Directory structure:"
echo "  - App directory: /opt/servantin"
echo "  - Data: /opt/servantin/data"
echo "  - Logs: /opt/servantin/logs"
echo "  - Backups: /opt/servantin/backups"
echo ""
log_info "Firewall ports open:"
echo "  - SSH (22)"
echo "  - HTTP (80)"
echo "  - HTTPS (443)"
echo "  - Backend API (8080)"
echo "  - Frontend Dev (3000)"
echo ""
log_warn "IMPORTANT: Log out and log back in for docker group changes to take effect"
log_info "Next step: Run the deploy.sh script to deploy the application"
echo ""

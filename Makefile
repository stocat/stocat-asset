# ANSI 색상 코드 (TUI/터미널 가독성용)
BLUE   = \033[1;34m
GREEN  = \033[1;32m
YELLOW = \033[1;33m
RED    = \033[1;31m
RESET  = \033[0m

.PHONY: help infra-up infra-down ps scraper websocket crawler all clean-ports stop-all log-scraper log-websocket log-crawler

help:
	@echo "$(BLUE)Stocat Asset Microservices 관리 도구$(RESET)"
	@echo "사용 가능한 명령:"
	@echo "  $(GREEN)infra-up$(RESET)        Docker Compose를 이용한 인프라(Redis, MySQL) 기동"
	@echo "  $(GREEN)infra-down$(RESET)      모든 인프라 컨테이너 종료 및 삭제"
	@echo "  $(GREEN)ps$(RESET)              실행 중인 컨테이너 및 프로세스 상태 확인"
	@echo "  $(YELLOW)clean-ports$(RESET)     기존 앱 프로세스 종료 (18080, 8082 포트)"
	@echo "  $(YELLOW)scraper$(RESET)         Asset Scraper 서비스 백그라운드 실행"
	@echo "  $(YELLOW)websocket$(RESET)       Asset WebSocket API 서비스 백그라운드 실행"
	@echo "  $(YELLOW)crawler$(RESET)         Toss 환율 크롤러 백그라운드 실행"
	@echo "  $(CYAN)log-<service>$(RESET)    서비스 로그 확인 (scraper, websocket, crawler)"
	@echo "  $(RED)stop-all$(RESET)         모든 백그라운드 서비스 종료"
	@echo "  $(BLUE)all$(RESET)             인프라 기동 및 모든 서비스 실행"

# --- 1. Infrastructure (인프라 계층) ---
infra-up:
	@echo "$(BLUE)[infra] 인프라를 기동합니다...$(RESET)"
	docker-compose up -d

infra-down:
	@echo "$(RED)[infra] 모든 구성을 종료합니다...$(RESET)"
	docker-compose down

ps:
	@echo "$(BLUE)[infra] 컨테이너 상태:$(RESET)"
	@docker-compose ps
	@echo "$(BLUE)[app] 백그라운드 프로세스 상태:$(RESET)"
	@pgrep -fl "bootRun|python" || echo "실행 중인 앱 프로세스가 없습니다."

clean-ports:
	@echo "$(RED)[clean] 기존 프로세스 정리 중...$(RESET)"
	@lsof -i :18080 -t | xargs kill -9 2>/dev/null || true
	@lsof -i :8082 -t | xargs kill -9 2>/dev/null || true
	@echo "$(GREEN)[clean] 정리 완료$(RESET)"

stop-all: clean-ports
	@echo "$(RED)[stop] 모든 백그라운드 서비스를 종료합니다...$(RESET)"
	@-pkill -f "bootRun" || true
	@-pkill -f "toss_exchange_rate_line.py" || true
	@echo "$(GREEN)[stop] 종료 완료$(RESET)"

# --- 2. Applications (애플리케이션 계층) ---
scraper: clean-ports
	@mkdir -p logs
	@echo "$(YELLOW)[app] Asset Scraper 백그라운드 실행 중...$(RESET)"
	@nohup ./gradlew :asset-scraper:bootRun > logs/asset-scraper.log 2>&1 &
	@echo "$(GREEN)🚀 Scraper가 시작되었습니다. 로그 확인: make log-scraper$(RESET)"

websocket: clean-ports
	@mkdir -p logs
	@echo "$(YELLOW)[app] Asset WebSocket API 백그라운드 실행 중...$(RESET)"
	@nohup ./gradlew :asset-websocket-api:bootRun > logs/asset-websocket-api.log 2>&1 &
	@echo "$(GREEN)🚀 WebSocket API가 시작되었습니다. 로그 확인: make log-websocket$(RESET)"

crawler:
	@mkdir -p logs
	@echo "$(YELLOW)[app] 환율 크롤러 백그라운드 실행 중...$(RESET)"
	@nohup $(MAKE) -C exchange-rate-crawler run > logs/exchange-rate-crawler.log 2>&1 &
	@echo "$(GREEN)🚀 Crawler가 시작되었습니다. 로그 확인: make log-crawler$(RESET)"

# --- 3. Logging (로그 계층) ---
log-scraper:
	@tail -f logs/asset-scraper.log

log-websocket:
	@tail -f logs/asset-websocket-api.log

log-crawler:
	@tail -f logs/exchange-rate-crawler.log

# --- 4. Orchestration (복합 실행) ---
all: infra-up stop-all
	@echo "$(GREEN)[all] 모든 서비스를 순차적으로 실행합니다...$(RESET)"
	@$(MAKE) scraper
	@$(MAKE) websocket
	@$(MAKE) crawler

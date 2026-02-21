# Toss Invest 실시간 환율 모니터링 Makefile (macOS 안전 버전)
# 사용법:
#   make run    → 가상환경 자동 생성 + 패키지 설치 + 실행
#   make clean  → 가상환경/캐시 삭제

PYTHON := python3
VENV := venv
ACTIVATE := source $(VENV)/bin/activate
SCRIPT := toss_exchange_rate_line.py

.PHONY: run setup clean

# 전체 실행: venv 생성 → 패키지 설치 → 스크립트 실행
run: setup
	@echo "🚀 Toss 환율 실시간 모니터링 실행 중..."
	@$(ACTIVATE) && $(PYTHON) $(SCRIPT)

# 가상환경 및 의존성 준비
setup:
	@if [ ! -d "$(VENV)" ]; then \
		echo "📦 가상환경 생성 중..."; \
		$(PYTHON) -m venv $(VENV); \
	fi
	@echo "📦 의존 패키지 설치 중..."
	@$(ACTIVATE) && $(PYTHON) -m pip install --upgrade pip >/dev/null
	@$(ACTIVATE) && $(PYTHON) -m pip install selenium webdriver-manager >/dev/null
	@echo "✅ 가상환경 준비 완료!"

# 가상환경 및 캐시 정리
clean:
	@echo "🧹 정리 중..."
	@rm -rf $(VENV)
	@find . -type d -name "__pycache__" -exec rm -rf {} +
	@rm -rf ~/.wdm || true
	@echo "✅ 정리 완료!"

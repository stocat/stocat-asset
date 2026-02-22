# 💹 Toss Invest 실시간 원화 환율 크롤러

이 프로젝트는 **Toss Invest** 사이트(`https://www.tossinvest.com/indices/exchange-rate`)에서  
실시간으로 원화(USD/KRW) 환율 정보를 가져오는 간단한 Python + Selenium 스크립트입니다.

---

## ⚙️ 구성

```
.
├── Makefile
├── README.md
└── toss_krw_optimized.py
```

---

## 🚀 실행 방법

### 1️⃣ Chrome 및 Chromedriver 설치

**macOS (Homebrew)**
```bash
brew install --cask google-chrome
brew install chromedriver
```

**Ubuntu**
```bash
sudo apt install -y chromium-browser chromium-chromedriver
```

---

### 2️⃣ 가상환경 생성 및 의존성 설치

Makefile이 자동으로 처리합니다.  
별도 설정 없이 한 번만 실행하면 됩니다 👇

```bash
make setup
```

---

### 3️⃣ 환율 크롤러 실행

```bash
make run
```

실행 후 다음과 같이 5초마다 실시간 환율이 출력됩니다:

```
💹 Toss Invest 환율 추적 시작...

[2025-10-31 01:17:14] KRW: 1,433.77원
[2025-10-31 01:17:19] KRW: 1,433.81원
[2025-10-31 01:17:24] KRW: 1,433.74원
```

---

## 🧱 Makefile 설명

```makefile
VENV := .venv
PYTHON := $(VENV)/bin/python3

setup:
	python3 -m venv $(VENV)
	$(PYTHON) -m pip install --upgrade pip
	$(PYTHON) -m pip install selenium

run:
	@echo "💹 Toss Invest 실시간 원화 환율 추적 시작..."
	@$(PYTHON) toss_krw_optimized.py
```

- `make setup` → 가상환경 생성 + Selenium 설치
- `make run` → 크롤러 실행

---

## 🧠 참고

- 스크립트는 Chrome headless 모드로 실행 가능  
  (`toss_krw_optimized.py` 내부에서 `opts.add_argument("--headless=new")` 주석 해제)
- Toss 페이지가 React 기반이라 DOM 갱신이 잦음 →  
  **StaleElementReferenceException** 발생 시 자동 복구됨

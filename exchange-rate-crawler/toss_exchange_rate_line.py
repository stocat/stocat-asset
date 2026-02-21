# toss_krw_optimized.py
import re
import time
import requests
from datetime import datetime
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import StaleElementReferenceException, NoSuchElementException

URL = "https://www.tossinvest.com/indices/exchange-rate"
NUM_KRW_RE = re.compile(r"(\d{1,3}(?:,\d{3})*(?:\.\d+)?)\s*원")

def get_rate_text(driver, el):
    """JS로 textContent 읽기 (가장 빠르고 안정적)"""
    try:
        return driver.execute_script("return arguments[0].textContent || '';", el).strip()
    except StaleElementReferenceException:
        return ""

def find_rate_element(driver):
    """환율 컨테이너와 실제 숫자 span 재탐색"""
    container = WebDriverWait(driver, 10).until(
        EC.presence_of_element_located(
            (By.XPATH, "//*[contains(@class,'k1860k1')][.//span[contains(@class,'k1860k2')][contains(.,'환율')]]")
        )
    )
    value_el = container.find_element(By.CSS_SELECTOR, "span._1vxpe8f2")
    return value_el

def extract_number(text: str) -> str | None:
    """문자열에서 '1,433.95원' 형태 추출"""
    m = NUM_KRW_RE.search(text)
    return m.group(0) if m else None

if __name__ == "__main__":
    opts = webdriver.ChromeOptions()
    opts.add_argument("--start-maximized")
    # opts.add_argument("--headless=new")  # 백그라운드 실행 시 주석 해제
    driver = webdriver.Chrome(options=opts)

    try:
        driver.get(URL)
        WebDriverWait(driver, 20).until(EC.presence_of_element_located((By.TAG_NAME, "body")))
        time.sleep(2)

        # 1️⃣ 최초 1회 DOM 찾기
        rate_el = find_rate_element(driver)
        print("💹 Toss Invest 환율 추적 시작...\n")

        while True:
            try:
                txt = get_rate_text(driver, rate_el)
                val = extract_number(txt)
                if val:
                    print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] KRW: {val}")
                    # API에 전송
                    val_cleaned = val.replace(',', '').replace('원', '').strip()
                    payload = {
                        "currencyPair": "USDKRW",
                        "exchangeRate": float(val_cleaned),
                        "timestamp": datetime.now().isoformat()
                    }
                    try:
                        requests.post("http://localhost:8080/api/v1/exchange-rates", json=payload, timeout=2)
                    except Exception as e:
                        print(f"API 전송 실패: {e}")
                else:
                    print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] KRW: <값 미검출>")

            except (StaleElementReferenceException, NoSuchElementException):
                # 2️⃣ DOM 교체 시 재탐색 자동 복구
                print("⚠️  환율 DOM이 갱신됨 — 재탐색 중...")
                rate_el = find_rate_element(driver)

            time.sleep(1)

    except KeyboardInterrupt:
        print("\n🛑 사용자 종료 (Ctrl + C)")
    finally:
        driver.quit()

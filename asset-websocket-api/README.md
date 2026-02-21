# Asset WebSocket API

## 로컬 개발

```bash
make -C localhost all   # 또는 make -C localhost
```
- 위 명령은 Gradle 빌드 → `stocat/asset-websocket-api:local` 이미지 빌드 → kind(`stocat-local`) 로드 → Helm 배포까지 수행합니다(네임스페이스 `stocat`).
- 별도로 테스트하려면 `make -C localhost docker`, `make -C localhost kind-load`를 사용하세요.

## Docker 빌드/푸시

```bash
make boot
make docker   # stocat/asset-websocket-api:0.0.1
make push
```
- 태그 변경: `make TAG=0.0.2 push`.

## Helm 배포

- 로컬: 위 `make -C localhost` 또는 `make helm-local-asset-websocket-api`.
- 운영/스테이징: `cd ../helm && make deploy SERVICE=asset-websocket-api ENV=prod`.

## 로컬 웹소켓 테스트 가이드

로컬 환경 혹은 포트 포워딩 상태에서 손쉽게 실시간 환율 연동을 테스트할 수 있는 방법입니다.

* **엔드포인트:** `ws://localhost:8082/ws/exchange-rates` (포트는 실제 로컬 구동 포트에 맞게 변경해 주세요)

### 브라우저 툴로 10초만에 테스트하기 (가장 추천 👍)
1. 크롤러 파이썬 스크립트 실행 (`toss_exchange_rate_line.py`)
2. `asset-scraper`, `asset-websocket-api` 스프링 서버 구동
3. 웹 브라우저에서 [PieHost WebSocket Tester](https://piehost.com/websocket-tester) 에 접속
4. **URL** 입력 칸에 `ws://localhost:8082/ws/exchange-rates` 입력
5. **Connect** 클릭!
6. 실시간 환율 데이터 JSON이 초 단위로 출력되는 것을 바로 확인하실 수 있습니다.

### Postman 사용하기
- 최신 버전의 Postman에서는 `New` -> `WebSocket Request` 기능을 공식 지원합니다.
- 위 엔드포인트 URL을 입력하고 **Connect** 하시면 편리하게 디버깅이 가능합니다.

# Trade WebSocket API

## 로컬 개발

```bash
make -C localhost all   # 또는 make -C localhost
```
- 위 명령은 Gradle 빌드 → `stocat/trade-websocket-api:local` 이미지 빌드 → kind(`stocat-local`) 로드 → Helm 배포까지 수행합니다(네임스페이스 `stocat`).
- 별도로 테스트하려면 `make -C localhost docker`, `make -C localhost kind-load`를 사용하세요.

## Docker 빌드/푸시

```bash
make boot
make docker   # stocat/trade-websocket-api:0.0.1
make push
```
- 태그 변경: `make TAG=0.0.2 push`.

## Helm 배포

- 로컬: 위 `make -C localhost` 또는 `make helm-local-trade-websocket-api`.
- 운영/스테이징: `cd ../helm && make deploy SERVICE=trade-websocket-api ENV=prod`.

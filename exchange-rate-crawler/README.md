# Exchange Rate Crawler

## 로컬 개발

```bash
make -C localhost all   # 또는 make -C localhost
```
- 이미지를 `stocat/exchange-rate-crawler:local`로 빌드하고 `stocat-local` Kind 클러스터(네임스페이스 `stocat`)에 로드합니다.
- 단계별 실행은 `make -C localhost docker`, `make -C localhost kind-load`로 분리할 수 있습니다.

## Docker 빌드/푸시

```bash
make boot
make docker   # stocat/exchange-rate-crawler:0.0.1
make push
```
- 태그 지정: `make TAG=0.0.2 push` 등.

## Helm 배포

- 로컬: `make -C localhost` 또는 루트에서 `make helm-local-exchange-rate-crawler`.
- 운영/스테이징: `cd ../helm && make deploy SERVICE=exchange-rate-crawler ENV=prod`.

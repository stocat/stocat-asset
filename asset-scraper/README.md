# Asset Scraper

## 로컬 개발

```bash
make -C localhost all   # 또는 make -C localhost
```
- 위 명령은 `stocat/asset-scraper:local` 이미지를 빌드하고 `stocat-local` Kind 클러스터(네임스페이스 `stocat`)에 로드한 뒤 Helm으로 배포합니다.
- Docker만 빌드하거나 Kind 로드만 실행하고 싶으면 `make -C localhost docker` / `make -C localhost kind-load`.

## Docker 빌드/푸시

```bash
make boot
make docker   # stocat/asset-scraper:0.0.1
make push
```
- 태그는 `TAG=1.0.0 make push`처럼 덮어쓰기.

## Helm 배포

- 로컬: 위 `make -C localhost` 혹은 `make helm-local-asset-scraper`.
- 운영/스테이징: `cd ../helm && make deploy SERVICE=asset-scraper ENV=prod`.

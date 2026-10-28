# Daily News — GitHub 1회 설정 가이드

> 대상 저장소: `kim-hani/daily_news` · 기준: [plan.md §8, §10.2](./plan.md) · 작성일: 2026-10-28

매일 09:00(KST)에 실행되는 `.github/workflows/daily-news.yml`이 동작하려면 아래 설정이 한 번 필요합니다.
설정 전에는 워크플로가 **실패하지 않고 경고만 남긴 채 건너뜁니다**(`check` job). 그래서 설정을 미뤄도 매일 실패 메일이 오지 않습니다.

| 순서 | 할 일 | 왜 필요한가 |
|---|---|---|
| 1 | 저장소와 브랜치 준비 | 예약 실행은 기본 브랜치(main)에 있는 워크플로 파일 기준으로 동작한다 |
| 2 | 배포 키 생성·등록 | 일일 뉴스 커밋만 main 보호 규칙을 우회해 직접 push한다(FR-8.1) |
| 3 | Variables 등록 | 커밋 작성자를 내 계정으로 → 잔디(기여 그래프)에 반영(FR-8.2) |
| 4 | GitHub Pages 소스 설정 | `site/`를 웹페이지로 배포(FR-8.4) |
| 5 | main Ruleset | 사람의 변경은 PR + 테스트 통과로만 main에 반영(FR-8.0) |
| 6 | 수동 실행·확인 | 전체 흐름 점검(T-037) |

웹 화면 경로와 함께 같은 작업을 하는 `gh` CLI 명령도 적었습니다. 둘 중 편한 방법을 쓰면 됩니다.

---

## 1. 저장소와 브랜치
- public 저장소 `https://github.com/kim-hani/daily_news`는 이미 만들어져 있고 `main`, `feature/daily-news`가 push되어 있습니다.
- 기능 작업 PR이 main에 병합되어야 `daily-news.yml`이 main에 들어가고 예약 실행이 시작됩니다.
- Pages를 무료로 쓰려면 저장소가 public이어야 합니다.

## 2. 배포 키(Deploy key)
워크플로는 이 키로 저장소를 checkout하고 main에 push합니다. 비밀값은 이 키 하나뿐입니다(NFR-5).

1. 로컬에서 키를 만듭니다. 비밀번호(passphrase)는 비워 둡니다(워크플로가 입력할 수 없음).
   ```bash
   ssh-keygen -t ed25519 -C "daily-news deploy key" -f daily_news_deploy -N ""
   ```
   `daily_news_deploy`(개인키)와 `daily_news_deploy.pub`(공개키)가 생깁니다.
2. **공개키**를 등록합니다: Settings → Deploy keys → **Add deploy key**
   - Title: `daily-news`
   - Key: `daily_news_deploy.pub` 내용
   - **Allow write access 체크** (push에 필요)
3. **개인키**를 등록합니다: Settings → Secrets and variables → Actions → **New repository secret**
   - Name: `NEWS_DEPLOY_KEY`
   - Secret: `daily_news_deploy` 파일 전체(`-----BEGIN OPENSSH PRIVATE KEY-----`부터 끝 줄까지)
4. 등록이 끝나면 로컬의 키 파일 두 개를 지웁니다.

`gh` CLI로 하려면:
```bash
gh repo deploy-key add daily_news_deploy.pub --repo kim-hani/daily_news --title daily-news --allow-write
gh secret set NEWS_DEPLOY_KEY --repo kim-hani/daily_news < daily_news_deploy
rm daily_news_deploy daily_news_deploy.pub
```

## 3. Variables(커밋 작성자)
잔디에 반영되려면 커밋 이메일이 내 GitHub 계정에 연결된 주소여야 합니다. public 저장소라 커밋 이메일이 공개되므로 **GitHub noreply 주소**를 권장합니다.

1. noreply 주소 확인: Settings(계정) → Emails → "Keep my email addresses private" 아래의 `숫자ID+아이디@users.noreply.github.com`
   ```bash
   gh api user --jq '"\(.id)+\(.login)@users.noreply.github.com"'
   ```
2. 저장소 Settings → Secrets and variables → Actions → **Variables** 탭 → New repository variable
   - `NEWS_COMMIT_NAME`: 표시 이름(예: `Kim Hani`)
   - `NEWS_COMMIT_EMAIL`: 위 noreply 주소

```bash
gh variable set NEWS_COMMIT_NAME --repo kim-hani/daily_news --body "Kim Hani"
gh variable set NEWS_COMMIT_EMAIL --repo kim-hani/daily_news --body "<숫자ID>+kim-hani@users.noreply.github.com"
```

## 4. GitHub Pages
Settings → Pages → Build and deployment → **Source: GitHub Actions**

```bash
gh api -X POST repos/kim-hani/daily_news/pages -f build_type=workflow
```
- 첫 배포가 끝나면 주소는 `https://kim-hani.github.io/daily_news/`입니다(`sources.yml`의 `siteUrl`과 같아야 합니다).
- `docs/`는 웹에 공개되지 않습니다. 배포 대상은 `site/` 폴더뿐입니다.

## 5. main Ruleset(보호 규칙)
Settings → Rules → Rulesets → **New ruleset → New branch ruleset**

| 항목 | 값 |
|---|---|
| Ruleset name | `main` |
| Enforcement status | **Active** |
| Bypass list | **Add bypass → Deploy keys** (Always allow) |
| Target branches | **Include default branch** |
| Restrict deletions | 체크 |
| Require a pull request before merging | 체크(Required approvals 0 — 혼자 운영하는 저장소) |
| Require status checks to pass | 체크 → **Add checks → `test`** (CI 워크플로 `ci`의 job 이름. 화면에는 `ci / test`로도 보임) |
| Block force pushes | 체크 |

결과: 사람과 기능 변경은 PR(+ `test` 통과)로만 main에 들어가고, 일일 뉴스 커밋만 배포 키로 직접 들어갑니다.

`gh` CLI로 하려면:
```bash
cat > ruleset.json <<'EOF'
{
  "name": "main",
  "target": "branch",
  "enforcement": "active",
  "conditions": { "ref_name": { "include": ["~DEFAULT_BRANCH"], "exclude": [] } },
  "bypass_actors": [ { "actor_type": "DeployKey", "bypass_mode": "always" } ],
  "rules": [
    { "type": "deletion" },
    { "type": "non_fast_forward" },
    { "type": "pull_request", "parameters": {
        "required_approving_review_count": 0, "dismiss_stale_reviews_on_push": false,
        "require_code_owner_review": false, "require_last_push_approval": false,
        "required_review_thread_resolution": false } },
    { "type": "required_status_checks", "parameters": {
        "strict_required_status_checks_policy": false,
        "required_status_checks": [ { "context": "test" } ] } }
  ]
}
EOF
gh api -X POST repos/kim-hani/daily_news/rulesets --input ruleset.json
rm ruleset.json
```

### Bypass에 Deploy keys가 없을 때(R3 대안)
Bypass 목록에서 Deploy keys를 고를 수 없으면 개인 액세스 토큰(PAT)으로 push합니다.
1. Bypass list에 **Repository admin** 역할을 넣습니다.
2. Settings(계정) → Developer settings → Fine-grained tokens에서 이 저장소만, **Contents: Read and write** 권한으로 토큰을 만듭니다.
3. Secret `NEWS_PUSH_TOKEN`으로 등록하고, `daily-news.yml`의 checkout을 다음처럼 바꿉니다.
   ```yaml
   - uses: actions/checkout@v4
     with:
       ref: main
       token: ${{ secrets.NEWS_PUSH_TOKEN }}
   ```
   `check` job의 `DEPLOY_KEY`도 `secrets.NEWS_PUSH_TOKEN`으로 바꿉니다. 이 경우 비밀값은 배포 키 대신 PAT가 되고, 토큰 만료일을 관리해야 합니다.

## 6. 수동 실행과 확인(T-037)
1. Actions → **daily-news** → **Run workflow**(branch: main)
   ```bash
   gh workflow run daily-news.yml --repo kim-hani/daily_news --ref main
   gh run watch --repo kim-hani/daily_news
   ```
2. 아래를 확인합니다.

### 체크리스트
- [ ] Deploy keys에 `daily-news`가 **Read/write**로 있다
- [ ] Secret `NEWS_DEPLOY_KEY`가 있다
- [ ] Variables `NEWS_COMMIT_NAME`, `NEWS_COMMIT_EMAIL`(noreply)이 있다
- [ ] Pages Source가 **GitHub Actions**다
- [ ] Ruleset `main`이 Active이고 Bypass에 Deploy keys가 있다
- [ ] 수동 실행에서 `check` → `build` → `deploy`가 모두 초록색이다(`check`만 돌고 끝나면 2·3번 설정 누락)
- [ ] main에 `news: YYYY-MM-DD daily newsletter` 커밋이 생겼고 작성자가 내 계정이다
- [ ] `news/YYYY/MM/YYYY-MM-DD.md`, `site/index.html`, README의 최신 뉴스레터 구간이 바뀌었다
- [ ] `https://kim-hani.github.io/daily_news/`가 열리고 오늘 뉴스레터가 보인다
- [ ] 다음 날 프로필 잔디에 커밋이 반영됐다
- [ ] 로컬에서 main에 직접 push하면 거부된다(`git push origin main` → rule violation)

## 문제 해결
| 증상 | 원인과 조치 |
|---|---|
| `check`만 실행되고 경고가 남는다 | `NEWS_DEPLOY_KEY`·`NEWS_COMMIT_NAME`·`NEWS_COMMIT_EMAIL` 중 빠진 것이 있다 |
| push 단계에서 `rule violation`/`permission denied` | 배포 키에 write 권한이 없거나 Ruleset Bypass에 Deploy keys가 없다 |
| `deploy` job이 `environment protection rules`로 실패 | Settings → Environments → `github-pages`의 배포 브랜치 규칙에 `main`을 허용한다 |
| Generate newsletter가 종료 코드 1 | 모든 피드 수집 실패(사이트 장애·차단). 로그의 `수집 실패` 줄을 확인한다. 커밋·배포는 하지 않는다 |
| 경향 기사가 모두 "RSS 요약" | 경향 사이트의 빈도 제한(403). `sources.yml`의 `hostDelayMillis.www.khan.co.kr`를 늘린다 |
| 실행이 수십 분 늦게 시작 | GitHub Actions 예약 실행의 정상 지연(A2). 반복되면 cron을 앞당긴다 |

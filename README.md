# 환생의 월드 (Reincarnation World)

기획서 ver. 17.0 기반 Paper/Folia 1.20.4 멀티 모듈 한국형 판타지 RPG 서버.

20개 커스텀 플러그인이 13개 세계(판타지/사이버펑크/해양/정령/명계/무협 등) 위에서
환생·신앙·왕국·기술·전쟁을 동시에 굴린다.

자세한 설계는 [MASTER_PLAN.md](MASTER_PLAN.md), 단계별 진행은 [ROADMAP.md](ROADMAP.md) 참고.

## 플러그인 구조 (총 20개)

```
RebornCore           DB · 스탯 · 경지 · 이벤트버스 · Folia 스케줄러 · GUI · KV · Lang
├── RebornNPC        NPC AI · 감정 · 관계 · 대화 · 전투 · 자율행동
├── RebornMob        세계별 몬스터 · 보스 · 스폰 · 드랍
├── RebornSpawn      환생 · 룰렛 · 초기스탯 · 헬스장 · NPC 자녀
├── RebornTutorial   13세계 튜토리얼 · 보호구역 · 이스터에그
├── RebornStat       세계별 성장 전략(판타지/사이버/해양/정령/명계/무협 등) · 경지 · 미니게임
├── RebornSkill      스킬 · 전투 · 13세계 에너지 · 비급학습 · 스킬창조
├── RebornQuest      NPC · 월드 · 발견 · 자기생성 · 세력 퀘스트
├── RebornEconomy    13세계 화폐 · 환전 · 상점 · 경매 · 거래
├── RebornDeath      사망 · 명계 · 윤회 · 범죄 · 현상수배
├── RebornTitle      칭호 6종 · 세계별/분야별/크로스 랭킹
├── RebornClan       가문 · 혈통 · 결혼 · 자녀 · 영토 · 왕국
├── RebornHiddenClass 히든 클래스 40종 (조건 엔진)
├── RebornCurse      축복 · 저주 · 세계 고유 디버프
├── RebornPet        펫 · 탈것 · 계약 (정령계 핵심)
├── RebornCraft      커스텀 아이템 · 7등급 · 세계별 제작 직업
├── RebornTime       현실 시간 동기화 · 세계 이동 · 시간의 방
├── RebornShip       해양제국 배 시스템 (등급 7단계)
├── RebornGod        신 · 신성 · 신역 · 교단 · 신전쟁
└── RebornWorldAI    13세계 자율 AI (경제 · 정치 · 몬스터 · 날씨)
```

## 빌드 & 배포

```sh
./gradlew build
```

각 플러그인 JAR은 `<plugin>/build/libs/` 에 생성된다. 빌드된 JAR을 서버
`plugins/` 디렉토리에 넣고 서버를 재시작하면 자동으로 로드된다.

플러그인 간 의존: `RebornCore`가 먼저 로드되어야 하고, 나머지 19개는 모두
`softdepend: RebornCore`. 표면적으로 순환 의존이 없도록 reflection + 이벤트 버스로
처리.

### 요구 사항

- Java 17+ (개발 시 21 toolchain 사용)
- Paper 1.20.4+ 또는 Folia 1.20.4+
- MySQL/MariaDB (또는 SQLite 폴백)
- ProtocolLib (NPC 패킷용 — 선택)

### 함께 권장되는 외부 플러그인 (인프라 11개)

Multiverse-Core, Multiverse-Portals, WorldEdit, FAWE, VoidGen, LuckPerms,
PlaceholderAPI, ProtocolLib, CoreProtect, Chunky, WorldPainter

## 주요 플레이어 명령

| 시스템 | 명령 | 비고 |
|---|---|---|
| 환생 | `/reborn`, `/roulette`, `/gym` | 룰렛으로 초기 스탯 결정 |
| 스탯 | `/stat`, `/breakthrough` | 경지 돌파 미니게임 |
| 스킬 | `/skill`, `/school gui` | 5대 무공 문파 GUI |
| 가문/왕국 | `/clan`, `/kingdom gui` | 결혼·자녀·영토·왕국 |
| 메가코프 | `/corp gui`, `/district gui` | 사이버펑크 7대 코프 점령전 |
| 해양제국 | `/empire gui`, `/port gui` | 해양 7대 제국 항구 점령전 |
| 종교 | `/god religion gui`, `/god pray` | 신앙·교단·의식 |
| 펫·탈것 | `/pet`, `/mount`, `/contract` | 정령계 핵심 |
| 화폐 | `/coin`, `/auction`, `/shop` | 13세계 화폐 |
| 죽음 | `/death`, `/wanted`, `/reincarnate` | 사망 처리 + 명계 윤회 |
| 시간 | `/calendar`, `/season`, `/timeroom` | 시간의 방 |
| 길잡이 | `/guide`, `/lang` | 세계 안내, 언어 전환 |

## 운영자 명령

| 명령 | 용도 |
|---|---|
| `/serverstat` | 전체 서버 메트릭 — 플레이어/세력/세계/메모리/TPS |
| `/serverstat <plugin>` | 특정 플러그인 상세 |
| `/corp mission <CORP> <type>`, `/empire mission ...` | 평판 보상 수동 부여 |
| `/kingdom <name> info`, `/god religion list` | 세력 상태 점검 |
| `/lang <ko\|en>` | 서버 기본 언어 전환 (admin) |

## 설정 (config.yml)

각 플러그인이 자체 `config.yml`을 가지며, 다음 항목들이 핵심 튜닝 포인트:

- **`RebornClan`**: `clan.create-min-total-stats`, `marriage.buff-stat-percent`,
  `child.request-success-chance`, `kingdom.cost`, `kingdom.tax-interval-hours`
- **`RebornGod`**: `ascend.faith-required`, `domain.upkeep-divinity-per-day`,
  `ritual.cooldown-hours / radius / duration-min`
- **`RebornStat`**: `takeover.district-threshold`, `takeover.port-threshold`,
  `takeover.max-influence`, `takeover.takeover-decay-pct`, `growth.fantasy.quest-mult`,
  `faction-reward.join-amount`, `faction-reward.rival-loss-pct`,
  `faction-reward.mission.*`, `faction-reward.empire-mission.*`
- **`RebornCore`**: `lang.default` (ko/en)

값을 바꾸고 `/reload`(가능한 플러그인) 또는 서버 재시작으로 반영.

## 핵심 설계 원칙

- **모든 게임 시스템 직접 구현** — 다른 플러그인의 API에 의존하지 않음
- **Paper/Folia 양쪽 호환** — `RebornScheduler`가 thread/region 자동 분기
- **config 우선** — 스탯·스킬·경지·NPC·몬스터·퀘스트·세력 보상 전부 YAML로 추가/수정
- **이벤트 버스 + reflection soft hook** — 표면적 순환 의존 0
- **세계별 독립 AI** — `RebornWorldAI`가 13개 세계 인스턴스를 독립적으로 운영

## 의존 그래프

```
RebornCore  (DB · 이벤트 · KV · GUI · Lang · 스케줄러)
   │
   ├── RebornStat ─── RebornSkill ─── RebornQuest
   │       │              │
   │       └── RebornTitle, RebornHiddenClass
   │
   ├── RebornNPC ─── RebornMob ─── RebornSpawn ─── RebornTutorial
   │
   ├── RebornClan ─── RebornDeath ─── RebornCurse ─── RebornPet
   │
   ├── RebornEconomy ─── RebornCraft ─── RebornShip
   │
   ├── RebornTime ─── RebornGod
   │
   └── RebornWorldAI (13개 세계 인스턴스)
```

`RebornCore`를 제외하고 모두 `softdepend`. 미설치 시 해당 기능만 비활성, 전체
서버는 정상 가동.

## 라이선스

비공개 프로젝트. 외부 배포 금지.

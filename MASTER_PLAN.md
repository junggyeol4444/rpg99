# 환생의 월드 — 전체 마스터 플랜 (20개 플러그인)

> 기획서 ver.17.0 전부를 **진짜 운영 가능한 수준**까지 단계별로 완성하는 전체 설계도.
> 각 단계는 독립적으로 빌드·검증 가능하며, "계속"이라고 하면 다음 단계를 만든다.

---

## 0. 현재 상태 (정직한 진단)

20개 플러그인 전부 **골격 + 데이터**는 있으나, 대부분 **로직이 얕다** (데이터는
YAML에 있는데 실제로 그 데이터대로 동작하는 코드가 없음).

| 플러그인 | Java(LOC) | YAML(LOC) | 상태 | 핵심 부족 |
|---|---|---|---|---|
| RebornCore | 1272 | 161 | ★★★★☆ | DB 영속화 일부, 이벤트 버스 OK |
| **RebornNPC** | 3886 | 411 | ★★★★☆ | **1.1~1.4 완료**, 1.5~1.6 남음 |
| RebornMob | 399 | 209 | ★★☆☆☆ | AI 10종 미구현 (스폰만 됨) |
| RebornSpawn | 393 | 59 | ★★★☆☆ | 룰렛 OK, 체육관·재굴림 얕음 |
| RebornTutorial | 226 | 45 | ★☆☆☆☆ | 세계별 튜토리얼 분기 없음 |
| RebornStat | 1006 | 200 | ★★★★☆ | 스탯 코어 OK, 성장곡선 검증 필요 |
| **RebornSkill** | 592 | 193 | ★★☆☆☆ | **136스킬 = 전부 "근처 1명 때리기"** |
| RebornQuest | 571 | 173 | ★★☆☆☆ | 분기·기여도·이벤트트리 얕음 |
| RebornEconomy | 1220 | 333 | ★★★☆☆ | 화폐 OK, 경매·시장 흐름 얕음 |
| RebornDeath | 677 | 53 | ★★★☆☆ | 사망 페널티 OK, 환생 연계 약함 |
| RebornTitle | 559 | 76 | ★★★☆☆ | 칭호 부여 OK, 효과 적용 얕음 |
| RebornClan | 864 | 489 | ★★★☆☆ | 데이터 풍부, 전쟁·정치 로직 없음 |
| RebornHiddenClass | 419 | 422 | ★★☆☆☆ | 45클래스 데이터, 해금조건 얕음 |
| RebornCurse | 514 | 240 | ★★★☆☆ | 저주 부여 OK, 해제·전파 얕음 |
| RebornPet | 331 | 47 | ★★☆☆☆ | 계약·탈것 골격만 |
| RebornCraft | 882 | 242 | ★★★☆☆ | 제작 OK, 강화·각인 얕음 |
| RebornTime | 308 | 215 | ★★★☆☆ | 달력 OK, 계절·천체 효과 얕음 |
| RebornShip | 480 | 34 | ★★★☆☆ | 물리 OK, 항해·교역 얕음 |
| RebornGod | 335 | 82 | ★★☆☆☆ | 신앙·교단 골격만 |
| RebornWorldAI | 596 | 60 | ★★☆☆☆ | 디렉터 골격, 재해·동적이벤트 없음 |

★ = 운영 가능 수준 근접도. 현재 **운영 가능한 건 0개**. RebornNPC가 가장 깊다.

**전체 남은 분량 추정: 약 25,000~30,000 LOC** (현재 ~16,000 → 목표 ~45,000).

---

## 진행 원칙

1. **세로로 깊게, 한 시스템씩** — 20개를 동시에 1%씩 올리지 않고, 한 시스템을 운영
   가능 수준까지 끌어올린 뒤 다음으로.
2. **의존 순서 존중** — Core → 전투/AI 기반 → 콘텐츠 → 세계 → 통합 → 폴리시.
3. **매 단계 = 1커밋 + 검증** — YAML/JSON 검증(샌드박스 Maven 차단으로 컴파일은
   사용자 환경에서). 단계마다 검증 방법 명시.
4. **재사용 우선** — 이미 만든 NPC 영혼·소문·관계망을 몹/퀘스트/세계가 공유.

---

# PHASE 1 — 기반 시스템 깊이 (모든 세계 공통)

전투·AI·퀘스트·경제는 13세계가 전부 공유한다. 여기를 먼저 운영 수준으로 만든다.

## Step 1 — 진짜 자율 NPC (RebornNPC) — 진행 중

- ✓ 1.0 Behavior 골격 / 1.1 성격·기억·욕구 / 1.2 목표 / 1.3 Utility 의사결정 / 1.4 사회망·소문
- ⏳ **1.5 Faction Dynamics** ← 다음
- ☐ 1.6 World Impact

### Step 1.5 — 파벌 형성·정치·전쟁
**목표:** 소문·관계망 위에 "집단"을 얹는다. NPC들이 스스로 세력을 만들고, 동맹/전쟁/
배신하고, 영토·자원을 두고 다툰다.
- 신규 `kr.reborn.npc.faction`: `Faction`(id, 지도자, 구성원, 영토중심, 국고, 적/동맹 목록,
  이념 vector), `FactionManager`(매주기 세력 형성·합병·붕괴), `FactionRelation`(전쟁/동맹/중립/조공).
- 형성 규칙: AMBITION 높은 무소속 NPC가 주변 관계망(FRIEND/ALLY 3명+) 모으면 세력 창설
  → GoalKind.FOUND_TOWN 완료 이벤트 연동.
- 전쟁: 두 세력의 nemeses 누적 + 자원 경쟁 → 선전포고 → 구성원에게 일괄 GoalKind.DESTROY_RIVAL_FACTION.
- 외교: 세력 지도자끼리 이념 vector 유사도 → 동맹/조공 협상 (SocialBehavior 확장).
- 검증: `/rnpc spawn` 10개 → 5분 후 `/rnpc faction list`에 자생 세력 2~3개 확인.

### Step 1.6 — World Impact (NPC가 세계를 바꿈)
**목표:** NPC 목표 완료가 **실제 월드 구조물·블록·제도**를 만든다. 지금은 추상 상태만 바뀜.
- FOUND_TOWN 완료 → 실제 마을 마커(베드/모닥불/표지판) 생성, 세력 영토 등록.
- FOUND_RELIGION 완료 → RebornGod에 신규 교단 등록 (이벤트 연동).
- ASCEND 완료 → RebornClan 왕국/RebornTitle 칭호 부여.
- START_BUSINESS 완료 → RebornEconomy에 NPC 상점 등록(실제 거래 가능).
- 검증: AMBITION 90 NPC 1마리 장기 관찰 → 마을→세력→왕국 단계 실제 반영 확인.

---

## Step 2 — 스킬 효과 진짜 구현 (RebornSkill)
**현 상태:** `SkillCaster.applyEffect()`가 스킬 종류와 무관하게 "정면 8블록 가장 가까운 1명
때리기"만 함. 136개 스킬이 전부 똑같이 동작.

**목표:** YAML의 `type`/`shape`/`element`별로 진짜 다르게 동작.
- 신규 `skill/effect/` 패키지 — 효과 전략 패턴:
  - `ProjectileEffect`(Snowball/Arrow 커스텀 메타데이터 추적 → 명중 시 데미지),
  - `AoeEffect`(반경 내 전체 + 파티클 링),
  - `BuffEffect`(PotionEffect + 커스텀 스탯 버프 N초),
  - `DashEffect`/`BlinkEffect`(이동기), `SummonEffect`(RebornMob 소환), `HealEffect`,
  - `DotEffect`(도트딜 — 스케줄러 틱), `ChainEffect`(연쇄 번개식).
- `element` 상성표 — FIRE>ICE>... HOLY<->DARK, 상성 시 ×1.5 / 역상성 ×0.66.
- 캐스팅 바(액션바 진행도) + 피격 시 시전 중단 + 콤보 윈도우.
- SkillDef.yml 스키마 확장: `type, shape, radius, projectileSpeed, element, durationTicks, ...`.
- 검증: 투사체/AOE/버프/이동기 각 1개 직접 시전해 동작 확인. 상성 데미지 로그.

## Step 3 — 몬스터 AI 10종 (RebornMob)
**현 상태:** 스폰만 됨(`SpawnTicker`), `MobAI`는 사실상 빈 골격. 132종 몬스터가 전부 바닐라 AI.

**목표:** 기획서 10종 아키타입 실동작.
- `mob/ai/` 전략: `PackAI`(리더 추종·협공), `BossAI`(HP% 페이즈 전환·패턴 스킬),
  `RangedAI`(거리 유지·카이팅), `AmbushAI`(은신→기습), `CasterAI`(스킬 시전),
  `SwarmAI`(다수 약체), `TankAI`(어그로 흡수), `SupportAI`(아군 힐/버프),
  `BerserkerAI`(HP 낮을수록 강화), `SummonerAI`(하수인 소환).
- 보스 페이즈: `BossManager` 확장 — 체력 75/50/25%에서 패턴 변화·브로드캐스트.
- RebornSkill 연동: 몬스터도 스킬 시전(SkillCaster 재사용).
- 검증: 각 AI별 대표 몹 1마리 스폰해 행동 관찰. 보스 페이즈 전환 확인.

## Step 4 — 퀘스트 엔진 깊이 (RebornQuest)
**현 상태:** `QuestEngine`/`EventTree` 골격. 선형 진행만, 분기·결과 반영 약함.

**목표:** 분기형 스토리 + 선택 결과가 세계에 반영.
- `EventTree` 실동작 — 노드(대사/선택지/조건/보상/분기), `EventChoiceCommand`로 선택.
- 조건 엔진 — 스탯/칭호/세계/관계/아이템 보유 검사.
- 동적 퀘스트 — RebornWorldAI/NPC 목표에서 자동 생성(예: NPC 세력 전쟁 → "참전 퀘스트").
- 기여도 — `ContributionTracker`로 다수 플레이어 협력 퀘스트 정산.
- 검증: 3분기 이벤트트리 1개 작성 → 선택별 다른 보상·후속 확인.

## Step 5 — 세계 디렉터 AI (RebornWorldAI)
**현 상태:** `WorldAI`/`NpcSimulator` 골격, 재해/동적이벤트 미발생.

**목표:** 세계가 "살아있게" — 주기적으로 상황 분석 → 재해·이벤트·세력 개입 결정.
- 분석 틱 — 세계 인구/세력 균형/플레이어 활동 집계 → `RebornWorldAIAnalysisEvent`.
- 재해 — 가뭄/역병/마물 침공/천재지변 (`RebornDisasterStartEvent`) + 날씨 연동.
- 동적 이벤트 — 보스 강림, 보물 출현, NPC 반란 → 퀘스트/몹 스폰 트리거.
- NPC 시뮬 — 비활성 청크의 NPC도 추상 시뮬(인구·경제 변동).
- 검증: `/worldai analyze` → 결정 로그. 재해 1종 강제 발생 → 효과 확인.

## Step 6 — 히든 클래스 해금 (RebornHiddenClass)
**현 상태:** 45클래스 데이터(YAML 422줄) 충실, `ConditionEngine` 검사 로직 얕음.

**목표:** 숨겨진 조건 충족 시 자동 해금·전직.
- `Condition` 타입 확장 — 누적킬/특정몹처치/아이템조합/세계방문/칭호/관계/시간대/저주보유 등.
- `ConditionListener` — 각종 이벤트 구독해 진척 누적(`PlayerProgress`).
- 해금 연출 + 전직 시 스탯/스킬 재배치(RebornStat/RebornSkill 연동).
- 검증: 1개 히든클래스 조건 만족시켜 자동 해금 토스트 확인.

## Step 7 — 경제 깊이 (RebornEconomy)
**현 상태:** 화폐·지갑 OK, 시장 흐름·경매 얕음.

**목표:** 자생 경제.
- 경매장 — 등록/입찰/낙찰/수수료, 만료 환불.
- 시장 시세 — 공급·수요 기반 가격 변동(NPC 상점 거래량 반영).
- NPC 상점 — Step 1.6 START_BUSINESS와 연동, 실제 매매.
- 화폐 흐름 — 세금·국고(RebornClan)·교역(RebornShip) 연결.
- 검증: 경매 1건 등록→입찰→낙찰. 대량 거래 후 시세 변동 확인.

## Step 8 — 나머지 기반 시스템 마감
나머지 11개 플러그인을 운영 수준으로 끌어올림. 각 ~1커밋.
- **8.1 RebornClan** — 전쟁/정치/위계/국고/영토 (Step 1.5 세력과 통합).
- **8.2 RebornGod** — 신앙치·교단·신성권능·기적 (Step 1.6 종교 연동).
- **8.3 RebornCraft** — 강화/각인/제작 트리/실패 페널티.
- **8.4 RebornCurse** — 저주 전파·해제 의식·세대 저주.
- **8.5 RebornPet** — 계약·성장·탈것 탑승·전투 동행.
- **8.6 RebornDeath** — 환생 연계(RebornSpawn), 사망 페널티 세계별 차등.
- **8.7 RebornTitle** — 칭호 효과 실적용(스탯/명성), 획득 조건.
- **8.8 RebornCraft/Ship/Time/Stat/Tutorial/Spawn** — 잔여 깊이 (계절효과·항해교역·세계별 튜토리얼 분기·재굴림 밸런스).
- 검증: 각 시스템 핵심 기능 1개씩 직접 동작.

---

# PHASE 2 — 13개 세계 콘텐츠 (지형 제외)

> 사용자 지시: **월드 지형(terrain)은 제외**. 지형 외 모든 콘텐츠는 만든다.
> Phase 1의 엔진(스킬/몹/퀘스트/세계AI)이 완성돼야 세계별 콘텐츠가 "진짜 동작"한다.

각 세계 = 1스텝. 세계별 공통 작업 항목:
1. **전용 몬스터** 8~15종 (RebornMob def + AI 배정).
2. **전용 NPC·세력** (RebornNPC 영혼 + RebornClan 시작 세력).
3. **전용 스킬/직업 색채** (RebornSkill 세계 한정 스킬).
4. **메인 퀘스트라인** (RebornQuest 이벤트트리).
5. **히든 클래스 1~3종** (RebornHiddenClass 조건).
6. **세계 고유 메커닉** (아래 표).
7. **세계 디렉터 시나리오** (RebornWorldAI 재해/이벤트 세트).

| Step | 세계 | 고유 메커닉 |
|---|---|---|
| 2.1 | FANTASY 판타지 | 마법·길드·던전, 왕국 정치 (가장 표준 → 먼저) |
| 2.2 | MARTIAL 무림 | 내공·문파·비급·기연 |
| 2.3 | DEMON 마계 | 마기·계급투쟁·마왕 승계 |
| 2.4 | HEAVEN 천계 | 신력·계율·타락 시스템 |
| 2.5 | SPIRIT 정령계 | 정령 계약·원소 조화·계절 동조 |
| 2.6 | IMMORTAL 선계 | 수련·천겁·우화등선 |
| 2.7 | YOKAI 요괴 | 요력·둔갑·인연 |
| 2.8 | EARTH 현대지구 | 총기·기업·도시 인프라 |
| 2.9 | MAGITECH 마공학 | 마나엔진·기계강화·공장 |
| 2.10 | APOCALYPSE 종말 | 방사능·생존·거점방어 |
| 2.11 | CYBERPUNK 사이버펑크 | 사이버웨어·해킹·기업전쟁 |
| 2.12 | DRAGON 용계 | 용화·브레스·보물둥지 |
| 2.13 | OCEAN 해양 | 잠수·해류·해적·RebornShip 핵심 무대 |

검증(세계별): 해당 세계 입장 → 전용 몹 조우, 메인퀘 1단계 진행, 히든클래스 조건 노출 확인.

---

# PHASE 3 — 세계 간 통합 (환생 루프)

기획서 핵심 = "죽으면 다른 세계로 환생". 세계들을 하나의 순환으로 묶는다.
- **3.1 환생 루프** — RebornDeath 사망 → RebornSpawn 룰렛 → 새 세계 + 전생 기억/칭호 일부 계승.
- **3.2 티어·차원 이동** — RebornCore TierManager 기반 세계 잠금·해금, 차원문.
- **3.3 크로스 세계 경제·외교** — 세력/교역/소문이 세계 경계를 넘는 조건(RebornShip/차원문).
- **3.4 6개 히든 월드** — ABYSS/UNDERWORLD/TIME_REALM/DREAM/VOID/GOD 진입 조건·고유 보상.
- 검증: 캐릭터 사망→환생→새 세계 시작, 계승 요소 확인. 히든월드 1곳 진입.

---

# PHASE 4 — 폴리시·운영 (출시 전)

- **4.1 영속화** — RebornCore DB에 NPC 영혼/관계망/세력/경제 상태 저장·복원 (서버 재시작 생존).
- **4.2 Folia 리전화** — 스케줄러 리전 스레드 안전성, 대규모 NPC 시뮬 성능.
- **4.3 밸런스 패스** — 스탯 성장곡선·스킬 데미지·경제 인플레 수치 조정.
- **4.4 어드민·운영 도구** — 통계 대시보드, 디버그 명령, 모니터링.
- **4.5 통합 테스트** — 신규 유저 온보딩~환생 전체 플로우 E2E.
- 검증: 재시작 후 상태 복원, 50+ NPC 동시 시뮬 TPS 측정.

---

## 요약: 전체 단계 순서

```
PHASE 1 (기반)
  Step 1  NPC      [1.0~1.4 ✓ | 1.5 세력 ⏳ | 1.6 월드임팩트]
  Step 2  스킬효과
  Step 3  몹 AI 10종
  Step 4  퀘스트 엔진
  Step 5  세계 디렉터 AI
  Step 6  히든클래스 해금
  Step 7  경제 깊이
  Step 8  나머지 11개 시스템 마감 (8.1~8.8)
PHASE 2 (세계)   2.1~2.13  13개 세계 콘텐츠
PHASE 3 (통합)   3.1~3.4   환생 루프·티어·히든월드
PHASE 4 (폴리시) 4.1~4.5   영속화·성능·밸런스·운영
```

**바로 다음:** Step 1.5 (Faction Dynamics) — "계속"이라고 하면 시작.

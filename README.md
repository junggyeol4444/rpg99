# 환생의 월드 (Reincarnation World)

기획서 ver. 17.0 기반 마인크래프트 Paper/Folia 멀티 모듈 플러그인 프로젝트.

## 구조 (총 20개 커스텀 플러그인)

```
RebornCore           DB·스탯·경지·이벤트버스·Folia 스케줄러
├── RebornNPC        NPC AI·감정·관계·대화·전투·자율행동
├── RebornMob        세계별 몬스터·보스·스폰·드랍
├── RebornSpawn      환생·룰렛·초기스탯·헬스장·NPC자녀
├── RebornTutorial   13세계 튜토리얼·보호구역·이스터에그
├── RebornStat       세계별 성장 전략·경지 돌파·미니게임
├── RebornSkill      스킬·전투·13세계 에너지·비급학습·스킬창조
├── RebornQuest      NPC·월드·발견·자기생성·세력 퀘스트
├── RebornEconomy    13세계 화폐·환전·상점·경매·거래
├── RebornDeath      사망·명계·윤회·범죄·현상수배
├── RebornTitle      칭호 6종·세계별/분야별/크로스 랭킹
├── RebornClan       가문·혈통·결혼·자녀·영토·왕국
├── RebornHiddenClass 히든 클래스 40종 (조건 엔진)
├── RebornCurse      축복·저주·세계 고유 디버프
├── RebornPet        펫·탈것·계약 (정령계 핵심)
├── RebornCraft      커스텀 아이템·7등급·세계별 제작 직업
├── RebornTime       현실 시간 동기화·세계 이동·시간의 방
├── RebornShip       해양제국 배 시스템 (등급 7단계)
├── RebornGod        신·신성·신역·교단·신전쟁
└── RebornWorldAI    13세계 자율 AI (경제·정치·몬스터·날씨)
```

## 빌드

```sh
./gradlew build
```

각 플러그인 JAR은 `<plugin>/build/libs/` 에 생성된다.

### 요구 사항
- Java 17+ (개발 시 21 toolchain 사용)
- Paper 1.20.4+ 또는 Folia 1.20.4+
- MySQL/MariaDB (또는 SQLite 폴백)
- ProtocolLib (NPC 패킷 — 선택)

### 다운받을 외부 플러그인 (인프라용 11개)
Multiverse-Core, Multiverse-Portals, WorldEdit, FAWE, VoidGen,
LuckPerms, PlaceholderAPI, ProtocolLib, CoreProtect, Chunky, WorldPainter

## 의존 그래프

`RebornCore` → 모든 플러그인이 의존.
이벤트 기반으로 순환 의존을 피하며, 런타임 hook은 reflection/
PluginManager로 soft-depend 처리.

## 핵심 설계 원칙

- 모든 게임 시스템은 직접 구현 (다른 플러그인 안 씀)
- Paper/Folia 양쪽 호환 (RebornScheduler가 자동 분기)
- config 우선 (스탯·스킬·경지·NPC·몬스터·퀘스트 모두 YAML로 추가/수정)
- 이벤트 버스 (RebornCore가 표준 이벤트 정의)
- 세계별 독립 AI (RebornWorldAI가 13개 세계 인스턴스 운영)

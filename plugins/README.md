# 외부 플러그인 (인프라·도구·유틸)

기획서 ver.17.0의 "다운받을 플러그인 11개" 중 마인크래프트 서버 플러그인 10개를
이 디렉토리에 둔다. (WorldPainter는 외부 데스크탑 프로그램이라 별도)

## 이미 포함된 5개 (GitHub Releases에서 직접 받음)

| 파일 | 버전 | 출처 |
|---|---|---|
| `Multiverse-Core.jar` | 4.3.16 | https://github.com/Multiverse/Multiverse-Core/releases |
| `Multiverse-Portals.jar` | 4.2.2 | https://github.com/Multiverse/Multiverse-Portals/releases |
| `ProtocolLib.jar` | 5.4.0 | https://github.com/dmulloy2/ProtocolLib/releases |
| `FastAsyncWorldEdit.jar` | 2.9.2 | https://github.com/IntellectualSites/FastAsyncWorldEdit/releases |
| `PlaceholderAPI.jar` | 2.12.2 | https://github.com/PlaceholderAPI/PlaceholderAPI/releases |

## 별도 다운로드 필요한 5개 (자체 CDN)

다음 5개는 GitHub Releases에 jar 자산을 올리지 않고 자체 빌드 서버/CDN으로
배포한다. 아래 스크립트 또는 수동 다운로드:

```sh
./download-remaining.sh
```

| 파일 | 출처 |
|---|---|
| `LuckPerms.jar` | https://luckperms.net/download |
| `CoreProtect.jar` | https://www.coreprotect.net/downloads/ |
| `Chunky.jar` | https://hangar.papermc.io/pop4959/Chunky |
| `WorldEdit.jar` | https://enginehub.org/worldedit/ |
| `VoidGen.jar` | https://www.spigotmc.org/resources/voidgen.74599/ |

## WorldPainter (외부 도구)

마인크래프트 서버 plugin이 아니라 데스크탑 지형 편집 도구.
13개 세계 + 히든 6개 + 튜토리얼 13개 = 32개 월드의 실제 지형을 만들 때 사용.
https://www.worldpainter.net/ 에서 다운로드.

## 서버 배치

Paper 또는 Folia 서버의 `plugins/` 디렉토리에 위 jar들을 모두 복사:

```sh
cp plugins/*.jar /path/to/your/server/plugins/
# 그리고 Reborn 플러그인 20개 (각 모듈 build/libs/)도 함께 배치
find . -path "*/build/libs/Reborn*.jar" -exec cp {} /path/to/your/server/plugins/ \;
```

## 의존 관계

```
Multiverse-Core ──── Multiverse-Portals (선택)
                ┐
RebornCore ─────┴── RebornTime (월드 이동·시간의 방)

ProtocolLib ─── RebornNPC (패킷 NPC, 없으면 일반 엔티티 fallback)

FastAsyncWorldEdit ─── (WorldEdit 의존. 빌드 시 둘 다 필요)
WorldEdit ──────────── (실제로 받을 때 FAWE만 받아도 WorldEdit이 함께 동작)

LuckPerms ─── (모든 권한 — rebornXXX.* 권한 노드)
PlaceholderAPI ─── (다른 plugin이 %reborn_stat_strength% 같은 placeholder 사용)
CoreProtect ─── (블록 로그·롤백, 운영 안정성)
Chunky ─── (월드 프리로드, 서버 부담 감소)
VoidGen ─── (빈 월드 생성, 신역·꿈의 세계 등)
```

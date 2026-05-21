# 환생의 월드 — 진짜 운영 가능 수준 ROADMAP

기획서 ver.17.0 전부를 실제 동작 가능한 수준까지 단계별로 완성.

## Phase 1 — 기반 시스템 깊이 (모든 세계 공통)

### Step 1: 진짜 자율 NPC (sub-step으로 분할)

지적: "단순 if문 8개 = 마크 빌리저 + α일 뿐"
→ 진짜 NPC는 성격·기억·욕구·장기목표를 가지고 **스스로** 결정해야 함.

- ✓ Step 1.0: Behavior 골격 (이전 — 단순 priority 기반)
- ⏳ **Step 1.1: Personality + Memory + Needs** (현재)
- Step 1.2: Goals (장기 목표 — 야망·사랑·복수·창조)
- Step 1.3: Utility-based 의사결정 (성격·욕구·기억·목표 종합)
- Step 1.4: Social Network (관계 그래프·소문 전파)
- Step 1.5: Faction Dynamics (파벌 형성·정치·전쟁)
- Step 1.6: World Impact (NPC가 진짜로 가게·종교·왕국 만들어냄)

### Step 2: 스킬 효과 진짜 구현
- 투사체 (Snowball/Arrow custom 기반)
- AOE 범위 공격
- 속성 상성 (FIRE vs ICE, HOLY vs DARK 등)
- 캐스팅 바·시전 중단·콤보
- 파티클·사운드 이펙트

### Step 3: 몬스터 AI 10종 진짜 동작
- PACK: 리더 따라 무리 행동
- BOSS: HP % 페이즈 전환
- RANGED: 거리 유지 + 후퇴
- FLEE/TERRITORIAL: 영역 시스템
- FLYING/AQUATIC: 공간별 이동
- 영물·흉수·요마 등 무협 분류

### Step 4: 퀘스트 12종 타입 전부 작동
- KILL, GATHER, TALK, MOVE, ESCORT, CRAFT
- SKILL_USE, SURVIVE, EXPLORE, DEFEND, DELIVER, CUSTOM
- 단계별 진행, 추적 ActionBar

### Step 5: 세계 AI 실제 데이터 분석
- RebornEconomy 실제 거래 로그 분석
- RebornClan 실제 세력 관계 분석
- RebornMob 실제 몬스터 카운트 분석
- 결정 → 실제 NPC 군대 편성·이동

### Step 6: 히든 클래스 40종 passive 효과 진짜 구현
- DRAGON_GROW_X2, GATE_BOOST_20 등 실제 적용
- 스킬 자동 해금
- 면역 시스템 (cyber psychosis 등)

## Phase 2 — 13세계 각자 깊이

### Step 7: 판타지계 — 왕국·마법·검술·던전·마왕령
### Step 8: 마계 — 7대 마왕령·72귀족·마기·영혼 거래
### Step 9: 천계 — 9층·4대천사·신마전쟁
### Step 10: 정령계 — 정령 플레이어·4정령왕·12소정령·계약
### Step 11: 무협계 — 마교·정파·사파·비급 70종·도력
### Step 12: 선계 — 36동천·72복지·천겁·인약사 트리
### Step 13: 요계 — 백귀야행·보름달·변신·구미호
### Step 14: 지구 — 게이트 등급·미궁 100층
### Step 15: 마도공학계 — 7대 도시·마도 기기
### Step 16: 아포칼립스계 — 3대 세력·생존
### Step 17: 사이버펑크계 — 7대 메가코프·사이버네틱스
### Step 18: 드래곤계 — 5대 가문·시간의 방·새끼용 성장
### Step 19: 해양제국계 — 7대 제국·배·해전·심해

## Phase 3 — 크로스 시스템

### Step 20: 다세계 월드 퀘스트 (신마대전·대선마요 등)
### Step 21: 히든 월드 6개 깊이 (명계·심연·시간·꿈·허공·신계)
### Step 22: 결혼·자녀 깊이 (자녀 NPC 행동·혈통·전환)
### Step 23: 가문·왕국 깊이 (전쟁·외교·정략)
### Step 24: 신 시스템 깊이 (신앙·시련·신역·세계 창조)

## Phase 4 — 마무리

### Step 25: 패킷 NPC 진짜 구현
### Step 26: 리소스팩 (실제 텍스처·모델)
### Step 27: 부하 테스트·최적화

---

각 Step은 별도 커밋. 완료 시 ✓ 표시.

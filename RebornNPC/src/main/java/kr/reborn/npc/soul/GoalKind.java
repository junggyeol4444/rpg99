package kr.reborn.npc.soul;

/**
 * NPC가 추구하는 장기 목표의 종류.
 *
 * 각 종류는 NPC의 어느 성격·욕구가 만들어내는지 정의.
 * 예: AMBITION 70+ → GAIN_POWER 자동 생성
 *     친구 살해당함 → AVENGE 생성
 *     LOVE 욕구 30 미만 + 미혼 → FIND_LOVE 생성
 */
public enum GoalKind {

    // ─── 권력·사회적 ──────────────────────────
    GAIN_POWER       ("권력을 얻는다"),         // 가문 창설/영토/왕국/마왕/제독 등
    SERVE_LORD       ("주군을 섬긴다"),         // LOYALTY 높을 때, 강한 NPC에게 충성
    FOUND_TOWN       ("마을을 세운다"),         // 마을·정착지 건설
    FOUND_RELIGION   ("종교를 창시한다"),       // 신앙 만들기
    DEFEAT_RIVAL     ("라이벌을 꺾는다"),       // 특정 대상 격파

    // ─── 물질·경제적 ──────────────────────────
    GAIN_WEALTH      ("재물을 모은다"),         // 상점·자산
    START_BUSINESS   ("사업을 시작한다"),       // 상인 직업

    // ─── 정서적 ──────────────────────────
    FIND_LOVE        ("사랑을 찾는다"),         // 결혼
    PROTECT_FAMILY   ("가족을 지킨다"),         // 가족 위협 시
    AVENGE           ("복수한다"),              // 친구·가족 살해자 처단

    // ─── 자아실현 ──────────────────────────
    MASTER_ART       ("기예를 극한까지 수련한다"), // 무공·마법·도술
    EXPLORE          ("미지를 탐험한다"),       // 새로운 곳·것
    ACCUMULATE_KNOWLEDGE("지식을 쌓는다"),      // 서적·비급
    ASCEND           ("초월에 이른다"),         // 선인·신·용왕

    // ─── 음지 ──────────────────────────
    DESTROY_RIVAL_FACTION("적대 세력을 멸한다"), // 강한 세력 적대
    BETRAY           ("배신한다"),              // 충성이 매우 낮을 때
    HIDE             ("숨어 산다");             // 사교성 매우 낮음

    public final String description;
    GoalKind(String d) { this.description = d; }
}

package kr.reborn.npc.soul;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC의 기억 — 다른 존재(NPC·플레이어)와의 경험을 시간순으로 저장.
 *
 * 각 기억은:
 *   - subject: 누구에 대한 기억 (UUID 문자열 또는 NPC id)
 *   - kind: 기억의 종류 (도움받음·배신당함·살해당함 등)
 *   - intensity: 강도 1~100
 *   - timestamp: 발생 시각
 *   - decay: true면 시간에 따라 약해짐 (10일 후 사라짐)
 *
 * 기억은 성격·행동에 영향:
 *   - "X가 나를 도왔다" 기억이 강하면 X에 대한 우호도 증가
 *   - "X가 친구를 죽였다" 강한 기억은 평생 복수 동기
 *   - "X가 모욕했다" 기억은 PRIDE 높은 NPC에게 큰 영향
 */
public final class Memory {

    public enum Kind {
        HELPED_ME(+30),         // 도와줬다 → 우호도↑
        GIFTED_ME(+25),         // 선물했다
        SAVED_MY_LIFE(+80),     // 목숨 구해줬다
        HEALED_ME(+20),         // 치유해줬다
        TAUGHT_ME(+40),         // 가르쳐줬다
        BETRAYED_ME(-70),       // 배신했다 → 우호도↓
        ATTACKED_ME(-50),       // 공격했다
        INSULTED_ME(-25),       // 모욕했다
        KILLED_MY_FRIEND(-90),  // 친구를 죽였다 → 복수
        KILLED_MY_FAMILY(-100), // 가족을 죽였다 → 평생 복수
        STOLE_FROM_ME(-40),     // 훔쳤다
        BROKE_PROMISE(-35),     // 약속 어겼다
        ROMANCED_ME(+50),       // 구애했다
        MARRIED_ME(+90),        // 결혼했다
        DIVORCED_ME(-60),       // 이혼했다
        OPPOSED_ME(-15),        // 반대했다 (정치적)
        ALLIED_WITH_ME(+30);    // 동맹

        public final int defaultRelationDelta;
        Kind(int delta) { this.defaultRelationDelta = delta; }
    }

    public static final class Entry {
        public final String subject;   // NPC id 또는 player UUID
        public final Kind kind;
        public final int intensity;
        public final long timestamp;
        public final boolean decays;
        public final String context;   // 자유 텍스트 ("전쟁 중에", "축제에서" 등)

        public Entry(String subject, Kind kind, int intensity, long timestamp, boolean decays, String context) {
            this.subject = subject; this.kind = kind;
            this.intensity = intensity; this.timestamp = timestamp;
            this.decays = decays; this.context = context;
        }

        /** 현재 강도 — decay 적용 후. */
        public double currentIntensity() {
            if (!decays) return intensity;
            long ageMs = System.currentTimeMillis() - timestamp;
            long maxMs = 10L * 24 * 60 * 60 * 1000; // 10일
            if (ageMs >= maxMs) return 0;
            double k = 1.0 - (double) ageMs / maxMs;
            return intensity * k;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public void record(String subject, Kind kind, int intensity, String context) {
        // KILLED_MY_FAMILY 같은 강렬한 기억은 decay 없음
        boolean strong = kind == Kind.KILLED_MY_FAMILY
                || kind == Kind.SAVED_MY_LIFE
                || kind == Kind.MARRIED_ME
                || kind == Kind.KILLED_MY_FRIEND;
        entries.add(new Entry(subject, kind, intensity, System.currentTimeMillis(), !strong, context));
        // 100개 넘으면 가장 약한 것부터 제거
        if (entries.size() > 100) {
            entries.sort((a, b) -> Double.compare(a.currentIntensity(), b.currentIntensity()));
            entries.remove(0);
        }
    }

    public List<Entry> aboutSubject(String subject) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.subject.equals(subject) && e.currentIntensity() > 0.5) out.add(e);
        }
        return out;
    }

    /** subject에 대한 종합 감정 점수 (-100 ~ 100). 기억들의 intensity 합 + kind 의 default delta. */
    public double sentimentFor(String subject) {
        double sum = 0;
        for (Entry e : aboutSubject(subject)) {
            double cur = e.currentIntensity();
            sum += (cur / 100.0) * e.kind.defaultRelationDelta;
        }
        return Math.max(-100, Math.min(100, sum));
    }

    public boolean hasMemoryOf(String subject, Kind kind) {
        for (Entry e : aboutSubject(subject)) if (e.kind == kind) return true;
        return false;
    }

    public List<Entry> all() { return entries; }
}

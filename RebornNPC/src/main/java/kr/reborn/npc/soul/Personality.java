package kr.reborn.npc.soul;

import kr.reborn.core.util.Rand;

import java.util.EnumMap;

/**
 * NPC 고유 성격. 8가지 특성, 각 -100~100.
 * 행동·결정·관계 모든 것에 영향.
 *
 * 같은 상황에서도 성격에 따라 완전히 다른 결정.
 * 예: 모욕받으면
 *   - aggression 높음 → 즉시 공격
 *   - pride 높음 → 결투 신청
 *   - empathy 높음 → 용서
 *   - ambition 높음 → 기억해뒀다가 정치적 보복
 */
public final class Personality {

    public enum Trait {
        AMBITION,     // 야망 — 권력·지위 추구. 높으면 출세·정복 욕구
        AGGRESSION,   // 공격성 — 폭력 선호. 높으면 즉시 공격, 낮으면 회피
        GREED,        // 탐욕 — 재물·자원 추구
        LOYALTY,      // 충성 — 세력·가족 헌신
        EMPATHY,      // 공감 — 타인 배려. 높으면 도움·치유, 낮으면 무관심
        CURIOSITY,    // 호기심 — 새로운 것 탐구. 높으면 모험·연구
        PRIDE,        // 자존심 — 모욕 민감도
        GENEROSITY,   // 관대함 — 베풀기
        BRAVERY,      // 용감함 — 공포 저항
        SOCIABILITY   // 사교성 — 타인과 어울림 욕구
    }

    private final EnumMap<Trait, Integer> traits = new EnumMap<>(Trait.class);

    public Personality() {
        for (Trait t : Trait.values()) traits.put(t, 0);
    }

    /** 무작위 성격 생성 — 정규분포 비슷하게 평균 0 ± 35 정도 */
    public static Personality random() {
        Personality p = new Personality();
        for (Trait t : Trait.values()) {
            // 가우시안 근사 — 3개 평균
            int v = (Rand.range(-100, 100) + Rand.range(-100, 100) + Rand.range(-100, 100)) / 3;
            p.traits.put(t, v);
        }
        return p;
    }

    /** 직업별 성격 편향 — VILLAGER는 평범, WARRIOR는 aggression+, KING은 ambition+ */
    public static Personality fromJob(String job) {
        Personality p = random();
        switch (job) {
            case "KING": case "EMPEROR": case "DEMON_LORD":
                p.add(Trait.AMBITION, 50);
                p.add(Trait.PRIDE, 40);
                break;
            case "GUARD": case "WARRIOR":
                p.add(Trait.AGGRESSION, 30);
                p.add(Trait.BRAVERY, 40);
                p.add(Trait.LOYALTY, 30);
                break;
            case "MERCHANT":
                p.add(Trait.GREED, 40);
                p.add(Trait.SOCIABILITY, 30);
                break;
            case "PRIEST": case "MONK":
                p.add(Trait.EMPATHY, 40);
                p.add(Trait.GENEROSITY, 30);
                p.add(Trait.AGGRESSION, -30);
                break;
            case "ASSASSIN": case "THIEF":
                p.add(Trait.LOYALTY, -30);
                p.add(Trait.GREED, 30);
                break;
            case "SAGE": case "SCHOLAR":
                p.add(Trait.CURIOSITY, 50);
                p.add(Trait.AGGRESSION, -30);
                break;
            case "HERMIT":
                p.add(Trait.SOCIABILITY, -50);
                p.add(Trait.CURIOSITY, 30);
                break;
            case "FARMER": case "VILLAGER":
                p.add(Trait.AGGRESSION, -20);
                p.add(Trait.AMBITION, -20);
                break;
            default: break;
        }
        return p;
    }

    public int get(Trait t) { return traits.getOrDefault(t, 0); }
    public void set(Trait t, int v) { traits.put(t, Math.max(-100, Math.min(100, v))); }
    public void add(Trait t, int delta) { set(t, get(t) + delta); }

    /** 성격을 한 줄로 표현. 디버그·inspect용. */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        for (Trait t : Trait.values()) {
            int v = get(t);
            String c = v > 50 ? "§c" : v > 20 ? "§e" : v < -50 ? "§9" : v < -20 ? "§b" : "§7";
            sb.append(c).append(t.name().substring(0, 3)).append(v < 0 ? "" : "+").append(v).append(" ");
        }
        return sb.toString();
    }
}

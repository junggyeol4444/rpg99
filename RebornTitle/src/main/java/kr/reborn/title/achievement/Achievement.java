package kr.reborn.title.achievement;

import java.util.ArrayList;
import java.util.List;

/**
 * 업적 정의 — 영구 기록 + 점수 + 보상.
 *
 * 일반 칭호와 다른 점:
 *   - 한 번 달성하면 영구 보존 (회수 안 됨)
 *   - 업적 점수가 누적되어 메달 등 추가 보상
 *   - 진척도 추적 (예: 100/1000 마리 처치)
 *
 * 카테고리:
 *   COMBAT   — 처치 관련
 *   EXPLORE  — 탐험 관련
 *   SOCIAL   — 결혼·동료·NPC 관련
 *   ECONOMIC — 화폐·거래 관련
 *   PROGRESS — 경지·환생·성장 관련
 *   SPECIAL  — 특수 이벤트
 */
public final class Achievement {

    public enum Category {
        COMBAT, EXPLORE, SOCIAL, ECONOMIC, PROGRESS, SPECIAL
    }

    public enum Rarity {
        BRONZE  (10,  "&7"),
        SILVER  (50,  "&f"),
        GOLD    (100, "&6"),
        PLATINUM(500, "&b"),
        DIAMOND (2000,"&3"),
        LEGEND  (10000,"&5&l");

        public final int points;
        public final String color;
        Rarity(int p, String c) { this.points = p; this.color = c; }
    }

    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    public final Rarity rarity;
    public final int requiredProgress;
    public final List<String> rewards = new ArrayList<>();

    public Achievement(String id, String name, String description,
                       Category category, Rarity rarity, int requiredProgress) {
        this.id = id; this.name = name; this.description = description;
        this.category = category; this.rarity = rarity;
        this.requiredProgress = requiredProgress;
    }
}

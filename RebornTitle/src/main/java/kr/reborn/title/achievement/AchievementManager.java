package kr.reborn.title.achievement;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.title.RebornTitle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 업적 매니저.
 *
 * 50종 시드 업적 + 진척 추적 + 자동 부여 + 점수 누적.
 */
public final class AchievementManager {

    private final RebornTitle plugin;
    private final Map<String, Achievement> defs = new HashMap<>();
    /** uuid → 달성한 업적 set */
    private final Map<UUID, Set<String>> earned = new ConcurrentHashMap<>();
    /** uuid → 업적 진척도 (조건별) */
    private final Map<UUID, Map<String, Integer>> progress = new ConcurrentHashMap<>();
    /** uuid → 총 업적 점수 */
    private final Map<UUID, Integer> totalPoints = new ConcurrentHashMap<>();

    public AchievementManager(RebornTitle plugin) {
        this.plugin = plugin;
        seed();
    }

    private void seed() {
        // 전투 업적
        add("first_kill", "첫 사냥", "처음으로 적을 처치", Achievement.Category.COMBAT,
                Achievement.Rarity.BRONZE, 1);
        add("hundred_kills", "백인 살수", "100명의 적 처치", Achievement.Category.COMBAT,
                Achievement.Rarity.SILVER, 100);
        add("thousand_kills", "천 사냥꾼", "1000명 처치", Achievement.Category.COMBAT,
                Achievement.Rarity.GOLD, 1000);
        add("ten_thousand_kills", "만 사냥꾼", "10000명 처치", Achievement.Category.COMBAT,
                Achievement.Rarity.PLATINUM, 10000);
        add("boss_slayer", "보스 학살자", "보스 50체 처치", Achievement.Category.COMBAT,
                Achievement.Rarity.GOLD, 50);
        add("god_slayer", "신살자", "신 클래스 보스 처치", Achievement.Category.COMBAT,
                Achievement.Rarity.LEGEND, 1);

        // 탐험
        add("world_visit_3", "3세계 여행자", "3개 세계 방문", Achievement.Category.EXPLORE,
                Achievement.Rarity.BRONZE, 3);
        add("world_visit_7", "7세계 순례자", "7개 세계 방문", Achievement.Category.EXPLORE,
                Achievement.Rarity.SILVER, 7);
        add("world_visit_13", "13세계 정복자", "모든 세계 방문", Achievement.Category.EXPLORE,
                Achievement.Rarity.GOLD, 13);
        add("dungeon_clearer", "던전 정복자", "던전 5개 클리어", Achievement.Category.EXPLORE,
                Achievement.Rarity.PLATINUM, 5);
        add("hidden_world_visitor", "히든 월드 방문자",
                "심연·명계·신계 등 6 히든 월드 방문", Achievement.Category.EXPLORE,
                Achievement.Rarity.DIAMOND, 6);

        // 사교
        add("first_marriage", "첫 결혼", "결혼했다", Achievement.Category.SOCIAL,
                Achievement.Rarity.SILVER, 1);
        add("clan_founder", "가문 창설", "가문 설립", Achievement.Category.SOCIAL,
                Achievement.Rarity.GOLD, 1);
        add("religious_leader", "교주", "교단 창설", Achievement.Category.SOCIAL,
                Achievement.Rarity.GOLD, 1);
        add("npc_lover", "민중의 인기인", "100 NPC와 호감도 70+", Achievement.Category.SOCIAL,
                Achievement.Rarity.PLATINUM, 100);
        add("pet_master", "펫 마스터", "5마리 동시 길들임", Achievement.Category.SOCIAL,
                Achievement.Rarity.GOLD, 5);

        // 경제
        add("first_million", "백만장자", "100만 GOLD 보유", Achievement.Category.ECONOMIC,
                Achievement.Rarity.SILVER, 1_000_000);
        add("billionaire", "억만장자", "10억 GOLD 보유", Achievement.Category.ECONOMIC,
                Achievement.Rarity.DIAMOND, 1_000_000_000);
        add("trade_king", "거래왕", "1000회 거래", Achievement.Category.ECONOMIC,
                Achievement.Rarity.GOLD, 1000);
        add("auction_master", "경매장 마스터", "10회 경매 낙찰", Achievement.Category.ECONOMIC,
                Achievement.Rarity.SILVER, 10);
        add("merchant_emperor", "상인 황제", "10세계에서 거래 성공",
                Achievement.Category.ECONOMIC, Achievement.Rarity.PLATINUM, 10);

        // 진척
        add("first_tier_up", "경지 승급", "첫 경지 도달", Achievement.Category.PROGRESS,
                Achievement.Rarity.BRONZE, 1);
        add("absolute", "절대자", "스탯 총합 5000 돌파", Achievement.Category.PROGRESS,
                Achievement.Rarity.GOLD, 1);
        add("god_ascent", "신 등극", "신이 되었다", Achievement.Category.PROGRESS,
                Achievement.Rarity.LEGEND, 1);
        add("reincarnation_1", "환생자", "1회 환생", Achievement.Category.PROGRESS,
                Achievement.Rarity.BRONZE, 1);
        add("reincarnation_10", "십회 환생자", "10회 환생", Achievement.Category.PROGRESS,
                Achievement.Rarity.PLATINUM, 10);
        add("reincarnation_100", "백회 환생자", "100회 환생", Achievement.Category.PROGRESS,
                Achievement.Rarity.LEGEND, 100);

        // 무공/마법 비급
        add("manual_5", "비급 수집가", "5종 비급 보유", Achievement.Category.SPECIAL,
                Achievement.Rarity.SILVER, 5);
        add("manual_legend_3", "전설 수집가", "3종 LEGENDARY 비급 보유",
                Achievement.Category.SPECIAL, Achievement.Rarity.DIAMOND, 3);

        // 결투
        add("duel_master", "결투의 챔피언", "결투 100승", Achievement.Category.COMBAT,
                Achievement.Rarity.PLATINUM, 100);
        add("honor_master", "명예의 화신", "명예 500 도달",
                Achievement.Category.SOCIAL, Achievement.Rarity.GOLD, 500);

        // 특수
        add("died_100_times", "죽음의 친구", "100회 사망", Achievement.Category.SPECIAL,
                Achievement.Rarity.PLATINUM, 100);
        add("immortal_revive", "불멸자", "IMMORTAL_REVIVE 능력 발동",
                Achievement.Category.SPECIAL, Achievement.Rarity.LEGEND, 1);
        add("nine_tails", "구미호", "9꼬리 도달", Achievement.Category.SPECIAL,
                Achievement.Rarity.LEGEND, 1);
        add("century_dragon", "백년용", "용 나이 100세",
                Achievement.Category.SPECIAL, Achievement.Rarity.GOLD, 100);
        add("ancient_dragon_age", "고룡 나이", "용 나이 2000세",
                Achievement.Category.SPECIAL, Achievement.Rarity.DIAMOND, 2000);
        add("tribulation_3", "삼차 천겁", "천겁 3회 통과", Achievement.Category.PROGRESS,
                Achievement.Rarity.PLATINUM, 3);
        add("enlightenment_9", "구단계 깨달음", "깨달음 9단계 도달",
                Achievement.Category.PROGRESS, Achievement.Rarity.LEGEND, 9);
        add("element_kings_all", "원소왕의 친구", "6 원소왕 모두 호감도 200+",
                Achievement.Category.SPECIAL, Achievement.Rarity.LEGEND, 6);
        add("implant_full", "완전 사이보그", "8 임플란트 슬롯 모두 채움",
                Achievement.Category.SPECIAL, Achievement.Rarity.PLATINUM, 8);
        add("chaos_core", "카오스 코어 획득", "CHAOS 코어 1개 획득",
                Achievement.Category.SPECIAL, Achievement.Rarity.DIAMOND, 1);
        add("hero_aura_visible", "영웅의 후광", "신 후계자 단계 패시브 발동",
                Achievement.Category.SPECIAL, Achievement.Rarity.DIAMOND, 1);

        // 특수: 환생 마일스톤
        add("crossed_all_worlds", "세계의 방랑자", "모든 세계에서 거주 경험",
                Achievement.Category.PROGRESS, Achievement.Rarity.DIAMOND, 13);
        add("evolved_pet_5", "진화의 친구", "펫 5마리 진화",
                Achievement.Category.SOCIAL, Achievement.Rarity.GOLD, 5);
        add("collected_30_titles", "칭호 수집가", "30 칭호 보유",
                Achievement.Category.SPECIAL, Achievement.Rarity.GOLD, 30);
    }

    private void add(String id, String name, String desc, Achievement.Category cat,
                     Achievement.Rarity rarity, int required) {
        defs.put(id, new Achievement(id, name, desc, cat, rarity, required));
    }

    /** 업적 진척도 증가. */
    public void incrementProgress(Player p, String achievementId, int delta) {
        Achievement def = defs.get(achievementId);
        if (def == null) return;
        Set<String> set = earned.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        if (set.contains(achievementId)) return;
        Map<String, Integer> map = progress.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        int cur = map.merge(achievementId, delta, Integer::sum);
        if (cur >= def.requiredProgress) {
            grant(p, achievementId);
        }
    }

    private static final String NS = "RebornTitle.achievement";

    public void grant(Player p, String achievementId) {
        Achievement def = defs.get(achievementId);
        if (def == null) return;
        Set<String> set = earned.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        if (set.contains(achievementId)) return;
        set.add(achievementId);
        int newPts = totalPoints.merge(p.getUniqueId(), def.rarity.points, Integer::sum);
        // 영속화 — earned set은 콤마 join, points는 따로
        try {
            kr.reborn.core.RebornCore.get().kv().put(NS, p.getUniqueId(), "earned",
                    String.join(",", set));
            kr.reborn.core.RebornCore.get().kv().putInt(NS, p.getUniqueId(), "points", newPts);
        } catch (Throwable ignored) {}
        Msg.send(p, def.rarity.color + "&l[업적 달성] §f" + def.name
                + " &7+§e" + def.rarity.points + " §7점");
        if (def.rarity == Achievement.Rarity.LEGEND || def.rarity == Achievement.Rarity.DIAMOND) {
            Bukkit.broadcastMessage("§6§l[" + def.rarity + " 업적] §f"
                    + p.getName() + " §7가 §6" + def.name + " §7달성!");
        }
        // 보상 적용 (간단화)
        for (String reward : def.rewards) {
            if (reward.startsWith("title:")) {
                plugin.titles().grant(p, reward.substring(6));
            }
        }
    }

    public Set<String> earnedOf(UUID p) {
        Set<String> s = earned.get(p);
        if (s != null) return s;
        // DB 로드
        String earnedStr = kr.reborn.core.RebornCore.get().kv().get(NS, p, "earned");
        if (earnedStr == null || earnedStr.isEmpty()) return java.util.Collections.emptySet();
        Set<String> loaded = new HashSet<>(java.util.Arrays.asList(earnedStr.split(",")));
        earned.put(p, loaded);
        return loaded;
    }

    public int pointsOf(UUID p) {
        Integer cached = totalPoints.get(p);
        if (cached != null) return cached;
        int pts = kr.reborn.core.RebornCore.get().kv().getInt(NS, p, "points", 0);
        totalPoints.put(p, pts);
        return pts;
    }

    public Map<String, Integer> progressOf(UUID p) {
        return progress.getOrDefault(p, java.util.Collections.emptyMap());
    }

    public Map<String, Achievement> all() { return defs; }
}

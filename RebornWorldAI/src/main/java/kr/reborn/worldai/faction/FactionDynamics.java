package kr.reborn.worldai.faction;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Rand;
import kr.reborn.worldai.RebornWorldAI;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 세력 역학 시뮬레이터.
 *
 * 각 세계의 세력(faction)을 추적:
 * - 영향력 (influence: 0~1000) — NPC 수, 영토, 자원에서 누적
 * - 군사력 (military)
 * - 외교 관계 (allies / enemies / neutrals)
 * - 야망 (ambition: -1.0=평화주의 ~ 1.0=정복욕)
 *
 * 매 사이클:
 * 1. 세력간 자동 동맹·결렬 (ambition × 영향력 차로 결정)
 * 2. 영향력 큰 세력이 약한 세력 정복 시도 (전쟁 이벤트 트리거)
 * 3. 봉기·내부 갈등 (영향력은 크지만 안정 낮을 때)
 */
public final class FactionDynamics {

    private final RebornWorldAI plugin;
    /** worldKey → factionId → Faction */
    private final Map<WorldKey, Map<String, Faction>> factions = new HashMap<>();

    public FactionDynamics(RebornWorldAI plugin) {
        this.plugin = plugin;
        seedDefaultFactions();
    }

    private void seedDefaultFactions() {
        // 각 세계의 주요 세력 시드 — config로 옮길 수 있지만 시작은 하드코딩
        seed(WorldKey.FANTASY, "kingdom_arcania",  "아카니아 왕국",     500, 400, 0.3);
        seed(WorldKey.FANTASY, "mage_tower",        "마법사 탑",         300, 200, -0.2);
        seed(WorldKey.FANTASY, "dark_alliance",     "암흑 동맹",         200, 300, 0.7);

        seed(WorldKey.DEMON,   "demon_lord_legion","마왕군",            700, 600, 0.8);
        seed(WorldKey.DEMON,   "rebel_demons",     "반역 마족",         200, 150, 0.2);

        seed(WorldKey.HEAVEN,  "celestial_court",  "천계 조정",         800, 500, -0.3);
        seed(WorldKey.HEAVEN,  "fallen_angels",    "타락 천사단",       150, 200, 0.6);

        seed(WorldKey.MARTIAL, "wulin_alliance",   "무림맹",            500, 400, 0.0);
        seed(WorldKey.MARTIAL, "demon_cult",       "마교",              400, 500, 0.9);
        seed(WorldKey.MARTIAL, "sapa_union",       "사파 연합",         300, 250, 0.5);
        seed(WorldKey.MARTIAL, "imperial_palace",  "황궁",              600, 700, 0.2);

        seed(WorldKey.IMMORTAL,"taoist_alliance",  "정파 선맹",         600, 300, -0.1);
        seed(WorldKey.IMMORTAL,"demon_immortals",  "마선",              400, 500, 0.7);

        seed(WorldKey.YOKAI,   "fox_clan",         "구미호 일족",       400, 300, 0.0);
        seed(WorldKey.YOKAI,   "oni_horde",        "오니 군단",         500, 600, 0.6);

        seed(WorldKey.EARTH,   "korean_assoc",     "헌터 협회",         500, 400, 0.0);
        seed(WorldKey.EARTH,   "gate_pirates",     "게이트 약탈단",     200, 250, 0.6);

        seed(WorldKey.MAGITECH,"machine_lord",     "기계 영주",         500, 600, 0.4);
        seed(WorldKey.MAGITECH,"gnome_engineers",  "노움 공학단",       300, 200, -0.2);

        seed(WorldKey.APOCALYPSE, "survivor_camp", "생존자 캠프",       300, 200, -0.1);
        seed(WorldKey.APOCALYPSE, "raider_clan",   "약탈단",            400, 500, 0.8);
        seed(WorldKey.APOCALYPSE, "warlord_state", "군벌 국가",         500, 700, 0.7);

        seed(WorldKey.CYBERPUNK,"megacorp_alpha",  "메가코프 알파",     800, 500, 0.3);
        seed(WorldKey.CYBERPUNK,"netbreakers",     "넷브레이커",        200, 100, -0.4);
        seed(WorldKey.CYBERPUNK,"street_gangs",    "스트릿 갱",         300, 250, 0.5);

        seed(WorldKey.DRAGON,  "elder_drakes",     "고룡회",            900, 800, -0.5);
        seed(WorldKey.DRAGON,  "young_drakes",     "젊은 용",           300, 400, 0.7);

        seed(WorldKey.OCEAN,   "atlantis_court",   "아틀란티스 궁정",   700, 500, -0.1);
        seed(WorldKey.OCEAN,   "pirate_brotherhood","해적 형제단",      400, 500, 0.8);

        seed(WorldKey.SPIRIT,  "spirit_kings",     "정령왕들",          800, 400, -0.3);
        seed(WorldKey.SPIRIT,  "broken_pacts",     "계약 파기자",       200, 300, 0.6);
    }

    private void seed(WorldKey w, String id, String name, double influence, double military, double ambition) {
        Faction f = new Faction(id, name, w, influence, military, ambition);
        factions.computeIfAbsent(w, k -> new HashMap<>()).put(id, f);
    }

    public void cycle(WorldKey world, double tension, double stability) {
        Map<String, Faction> map = factions.get(world);
        if (map == null || map.isEmpty()) return;
        List<Faction> list = new ArrayList<>(map.values());

        // 1. 자연 변동 — 영향력/군사력 ±5
        for (Faction f : list) {
            f.influence = clamp(0, 1000, f.influence + Rand.rangeD(-5, 5));
            f.military = clamp(0, 1000, f.military + Rand.rangeD(-3, 3));
        }

        // 2. 자동 동맹/결렬
        if (Rand.chance(0.1)) tryDiplomacy(list);

        // 3. 정복 시도 — 영향력 차가 클 때, 야망 높은 측이 시도
        if (tension > 60 && Rand.chance(0.08)) tryConquest(list);

        // 4. 봉기 — 안정 낮고 야망 낮은 세력 (눌린 자) 반란
        if (stability < 30 && Rand.chance(0.05)) tryRevolt(list);
    }

    private void tryDiplomacy(List<Faction> list) {
        if (list.size() < 2) return;
        Faction a = list.get(Rand.range(0, list.size() - 1));
        Faction b = list.get(Rand.range(0, list.size() - 1));
        if (a == b) return;
        // 야망 비슷 → 동맹, 야망 정반대 → 결렬
        double diff = Math.abs(a.ambition - b.ambition);
        if (diff < 0.3 && !a.allies.contains(b.id)) {
            a.allies.add(b.id);
            b.allies.add(a.id);
            a.enemies.remove(b.id);
            b.enemies.remove(a.id);
            Bukkit.broadcastMessage("§a§l[" + a.world + " 외교] §f"
                    + a.name + " §7과 §f" + b.name + " §a동맹 체결.");
        } else if (diff > 0.7 && !a.enemies.contains(b.id)) {
            a.enemies.add(b.id);
            b.enemies.add(a.id);
            a.allies.remove(b.id);
            b.allies.remove(a.id);
            Bukkit.broadcastMessage("§c§l[" + a.world + " 외교] §f"
                    + a.name + " §7과 §f" + b.name + " §c적대 선언.");
        }
    }

    private void tryConquest(List<Faction> list) {
        // 야망 + 군사력 상위 1, 영향력 하위 1 매칭
        Faction strongest = list.stream()
                .filter(f -> f.ambition > 0.3)
                .max((x, y) -> Double.compare(
                        x.military + x.ambition * 200, y.military + y.ambition * 200))
                .orElse(null);
        if (strongest == null) return;
        Faction weakest = list.stream()
                .filter(f -> f != strongest && !f.allies.contains(strongest.id))
                .min((x, y) -> Double.compare(x.influence, y.influence))
                .orElse(null);
        if (weakest == null) return;
        if (strongest.influence - weakest.influence < 100) return;

        // 정복 선포
        strongest.enemies.add(weakest.id);
        weakest.enemies.add(strongest.id);
        Bukkit.broadcastMessage("§4§l[" + strongest.world + " 정복] §f"
                + strongest.name + " §c⚔ §f" + weakest.name + " §7— 정복 전쟁 시작!");
        // 결과 즉시 계산 (확률 기반)
        double pStrong = strongest.military / (strongest.military + weakest.military * 1.2);
        if (Rand.chance(pStrong)) {
            strongest.influence += weakest.influence * 0.5;
            strongest.military += weakest.military * 0.3;
            weakest.influence *= 0.3;
            weakest.military *= 0.5;
            Bukkit.broadcastMessage("§6§l[전쟁 결과] §f"
                    + strongest.name + " §a승리. §f" + weakest.name + " §7복속·국력 손실.");
        } else {
            strongest.military *= 0.6;
            weakest.military *= 0.85;
            Bukkit.broadcastMessage("§e§l[전쟁 결과] §f"
                    + weakest.name + " §a방어 성공! §f" + strongest.name + " §7군 와해.");
        }
    }

    private void tryRevolt(List<Faction> list) {
        Faction oppressed = list.stream()
                .filter(f -> f.ambition < 0.2 && f.influence < 300)
                .min((x, y) -> Double.compare(x.influence, y.influence))
                .orElse(null);
        if (oppressed == null) return;
        // 봉기 시 영향력 +50, 야망 +0.3
        oppressed.influence += 50;
        oppressed.ambition = Math.min(1.0, oppressed.ambition + 0.3);
        oppressed.military += 30;
        Bukkit.broadcastMessage("§e§l[" + oppressed.world + " 봉기] §f"
                + oppressed.name + " §7가 들고 일어났다 — 영향력 +50, 군사 +30.");
    }

    public Collection<Faction> ofWorld(WorldKey w) {
        Map<String, Faction> map = factions.get(w);
        return map == null ? java.util.Collections.emptyList() : map.values();
    }

    public Faction get(WorldKey w, String id) {
        Map<String, Faction> map = factions.get(w);
        return map == null ? null : map.get(id);
    }

    public Map<WorldKey, Map<String, Faction>> all() { return factions; }

    private double clamp(double lo, double hi, double v) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static final class Faction {
        public final String id;
        public final String name;
        public final WorldKey world;
        public double influence;
        public double military;
        /** -1.0 (평화주의) ~ 1.0 (정복욕) */
        public double ambition;
        public final Set<String> allies = new HashSet<>();
        public final Set<String> enemies = new HashSet<>();
        /** 이 세력의 플레이어 멤버 (선택적 등록) */
        public final Set<UUID> players = new HashSet<>();

        public Faction(String id, String name, WorldKey world, double inf, double mil, double amb) {
            this.id = id; this.name = name; this.world = world;
            this.influence = inf; this.military = mil; this.ambition = amb;
        }
    }
}

package kr.reborn.time.calendar;

import kr.reborn.core.RebornCore;
import kr.reborn.time.RebornTime;
import org.bukkit.Bukkit;

/**
 * 환생력(달력) — 게임 내 날짜·달·년·주기.
 *
 * 1일 = 게임시간 24분 (config), 1달 = 30일, 1년 = 12달 = 360일.
 * 매일 자정 broadcast + 일별 이벤트 트리거.
 *
 * 주기 이벤트:
 *   - 7일마다: 주간 시장 (시세 -10%)
 *   - 30일마다: 보름달 (요계 보너스 ×2)
 *   - 90일마다: 분기 축제
 *   - 360일마다: 신년 (전체 NPC 호의 +5)
 */
public final class Calendar {

    private final RebornTime plugin;
    private long startedAt;

    public Calendar(RebornTime plugin) {
        this.plugin = plugin;
        this.startedAt = plugin.getConfig().getLong("calendar.started-at", System.currentTimeMillis());
        // 매 분 체크 (자정 도래 감지)
        RebornCore.get().scheduler().runTimer(this::tick, 1200L, 1200L);
    }

    public long elapsedMs() { return System.currentTimeMillis() - startedAt; }

    public int day() {
        long dayMs = plugin.getConfig().getLong("calendar.day-seconds", 1440) * 1000;
        return (int) (elapsedMs() / dayMs) + 1;
    }

    public int month() { return ((day() - 1) / 30) % 12 + 1; }
    public int year()  { return (day() - 1) / 360 + 1; }
    public int dayOfMonth() { return ((day() - 1) % 30) + 1; }
    public int dayOfYear()  { return ((day() - 1) % 360) + 1; }

    private int lastBroadcastDay = -1;
    /** 절기 24절 — 누적일 기준. */
    private static final String[] SEASONS = {
        "입춘 — 봄의 시작이다.", "우수 — 눈이 비가 된다.", "경칩 — 곤충이 깨어난다.",
        "춘분 — 낮과 밤이 같다.", "청명 — 하늘이 맑다.", "곡우 — 곡식에 비가 내린다.",
        "입하 — 여름이 시작된다.", "소만 — 만물이 가득 차오른다.", "망종 — 보리를 거둔다.",
        "하지 — 가장 긴 낮.", "소서 — 더위가 시작된다.", "대서 — 가장 더운 날.",
        "입추 — 가을이 시작된다.", "처서 — 더위가 물러난다.", "백로 — 이슬이 맺힌다.",
        "추분 — 낮과 밤이 같다.", "한로 — 찬 이슬.", "상강 — 서리가 내린다.",
        "입동 — 겨울이 시작된다.", "소설 — 첫 눈.", "대설 — 큰 눈.",
        "동지 — 가장 긴 밤.", "소한 — 추위가 시작된다.", "대한 — 가장 추운 날."
    };

    private void tick() {
        int d = day();
        if (d == lastBroadcastDay) return;
        lastBroadcastDay = d;
        // 매일 자정 broadcast + 절기 표시
        int seasonIdx = (d / 15) % 24;
        Bukkit.broadcastMessage("§e§l[환생력] §6Y" + year() + "M" + month() + "D" + dayOfMonth()
                + " §7(누적 " + d + "일) §8| §7" + SEASONS[seasonIdx]);
        // 7일 주간 시장 — PriceController.applyGlobalFactor(0.9) 호출
        if (d % 7 == 0) {
            Bukkit.broadcastMessage("§a§l[주간 시장] §7오늘 모든 시세 -10% — 상인들이 분주하다.");
            try {
                var ep = Bukkit.getPluginManager().getPlugin("RebornEconomy");
                if (ep != null) {
                    Object pc = ep.getClass().getMethod("priceController").invoke(ep);
                    if (pc != null) {
                        pc.getClass().getMethod("applyGlobalFactor", double.class)
                                .invoke(pc, 0.9);
                    }
                }
            } catch (Throwable ignored) {}
        }
        // 30일 보름달 — 누적 횟수 표시 + 요계 거주자에게 24시간 마커
        if (d % 30 == 0) {
            int moonNum = d / 30;
            Bukkit.broadcastMessage("§5§l[보름달] §7요계 보너스 ×2 §8— §7" + moonNum + "번째 보름달. 요괴가 활동을 시작한다.");
            try {
                long dayTicks = 24L * 3600L * 20L;
                for (var p : Bukkit.getOnlinePlayers()) {
                    var d2 = RebornCore.get().api().getPlayerData(p.getUniqueId());
                    if (d2 == null) continue;
                    if (d2.worldKey() == kr.reborn.core.data.WorldKey.YOKAI) {
                        d2.status().put("full_moon_bonus",
                                new kr.reborn.core.data.PlayerData.StatusEffect(
                                        "full_moon_bonus", "BLESSING", dayTicks, 1));
                        p.sendMessage("§5§l[보름달의 가호] §f요기 누적 ×2 (24시간)");
                    }
                }
            } catch (Throwable ignored) {}
        }
        // 90일 분기 축제 — 분기명 표시 + 모든 가문 treasury +1000
        if (d % 90 == 0) {
            int quarter = (d / 90) % 4;
            String[] festivals = {
                "춘제 — 봄의 축제. 만물이 깨어난다.",
                "하제 — 여름의 축제. 풍요가 넘친다.",
                "추제 — 가을의 축제. 수확을 기린다.",
                "동제 — 겨울의 축제. 한 해를 마감한다."
            };
            Bukkit.broadcastMessage("§e§l[" + festivals[quarter].split(" — ")[0] + "] §7"
                    + festivals[quarter].split(" — ")[1] + " §6모든 가문 treasury +1000");
            grantFestivalTreasury(1000.0);
        }
        // 360일 신년 — NPC 전체 호의 +5
        if (d % 360 == 0) {
            Bukkit.broadcastMessage("§6§l[신년] §fY" + year() + " 신년 — 전체 NPC 호의 +5");
            grantNewYearFavor(5.0);
        }
    }

    /** 분기 축제 — 모든 가문 treasury 추가. RebornClan 리플렉션 (Clan.treasury는 public 필드). */
    private void grantFestivalTreasury(double amount) {
        try {
            var cp = Bukkit.getPluginManager().getPlugin("RebornClan");
            if (cp == null) return;
            Object clans = cp.getClass().getMethod("clans").invoke(cp);
            if (clans == null) return;
            Object all = clans.getClass().getMethod("all").invoke(clans);
            if (all instanceof java.util.Collection<?> col) {
                for (Object clan : col) {
                    try {
                        var field = clan.getClass().getField("treasury");
                        double cur = field.getDouble(clan);
                        field.setDouble(clan, cur + amount);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    /** 신년 — 전체 NPC 호의 +값. RebornNPC 리플렉션. */
    private void grantNewYearFavor(double delta) {
        try {
            var np = Bukkit.getPluginManager().getPlugin("RebornNPC");
            if (np == null) return;
            np.getClass().getMethod("nudgeGlobalFavor", double.class)
                    .invoke(np, delta);
        } catch (Throwable ignored) {}
    }

    public String formatNow() {
        return "Y" + year() + " M" + month() + " D" + dayOfMonth() + " (총 " + day() + "일)";
    }
}

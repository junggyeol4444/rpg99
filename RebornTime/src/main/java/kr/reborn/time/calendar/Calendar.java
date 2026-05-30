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
    private void tick() {
        int d = day();
        if (d == lastBroadcastDay) return;
        lastBroadcastDay = d;
        // 매일 자정 broadcast
        Bukkit.broadcastMessage("§e§l[환생력] §6Y" + year() + "M" + month() + "D" + dayOfMonth()
                + " §7(누적 " + d + "일)");
        // 7일 주간 시장
        if (d % 7 == 0) {
            Bukkit.broadcastMessage("§a§l[주간 시장] §7오늘 모든 시세 -10%");
            // PriceController에 reflection으로 전체 카테고리 0.9 곱
            try {
                var ep = Bukkit.getPluginManager().getPlugin("RebornEconomy");
                if (ep != null) {
                    Object pc = ep.getClass().getMethod("priceController").invoke(ep);
                    if (pc != null) {
                        for (kr.reborn.core.data.WorldKey w : kr.reborn.core.data.WorldKey.values()) {
                            for (String cat : new String[]{"FOOD","METAL","MAGIC","WEAPON","ARMOR","RARE","INFO","MEDICINE"}) {
                                pc.getClass().getMethod("updateCategoryPrice",
                                                kr.reborn.core.data.WorldKey.class, String.class, double.class)
                                        .invoke(pc, w, cat, basePrice(cat) * 0.9);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        // 30일 보름달
        if (d % 30 == 0) {
            Bukkit.broadcastMessage("§5§l[보름달] §7요계 보너스 ×2 — 오늘 하루.");
        }
        // 90일 분기 축제
        if (d % 90 == 0) {
            Bukkit.broadcastMessage("§e§l[분기 축제] §7전 세계 모든 가문 treasury +1000");
        }
        // 360일 신년
        if (d % 360 == 0) {
            Bukkit.broadcastMessage("§6§l[신년] §fY" + year() + " 신년 — 전체 NPC 호의 +5");
        }
    }

    private double basePrice(String cat) {
        return switch (cat) {
            case "FOOD" -> 10;
            case "METAL" -> 100;
            case "MAGIC" -> 500;
            case "WEAPON" -> 300;
            case "ARMOR" -> 250;
            case "RARE" -> 5000;
            case "INFO" -> 200;
            case "MEDICINE" -> 50;
            default -> 100;
        };
    }

    public String formatNow() {
        return "Y" + year() + " M" + month() + " D" + dayOfMonth() + " (총 " + day() + "일)";
    }
}

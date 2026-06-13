package kr.reborn.clan.command;

import kr.reborn.clan.RebornClan;
import kr.reborn.clan.data.Clan;
import kr.reborn.core.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ClanCommand implements CommandExecutor {
    private final RebornClan plugin;
    public ClanCommand(RebornClan p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/clan create <id> <name> | join <id> | leave | invite <player> | info | list | treasury");
            Msg.send(p, "&7        | power list | power use <권능명>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "power": {
                if (a.length < 2 || (a.length >= 2 && "list".equalsIgnoreCase(a[1]))) {
                    Msg.send(p, "&6=== 알려진 권능 (" + plugin.powers().all().size() + ") ===");
                    int n = 0;
                    for (String name : plugin.powers().all()) {
                        if (n++ >= 30) { p.sendMessage("§7…"); break; }
                        p.sendMessage("  §7• §f" + name);
                    }
                    Msg.send(p, "&7사용: /clan power use <권능명>");
                    break;
                }
                if ("use".equalsIgnoreCase(a[1]) && a.length >= 3) {
                    StringBuilder name = new StringBuilder(a[2]);
                    for (int i = 3; i < a.length; i++) name.append(' ').append(a[i]);
                    if (!plugin.powers().use(p, name.toString())) {
                        Msg.error(p, "권능 없음 또는 쿨다운: " + name);
                    }
                }
                break;
            }
            case "create":
                if (a.length < 3) return true;
                plugin.clans().create(p, a[1], a[2]);
                break;
            case "join":
                if (a.length < 2) return true;
                Clan target = plugin.clans().get(a[1]);
                if (target == null) { Msg.error(p, "가문 없음"); break; }
                if (plugin.clans().join(target, p)) {
                    Msg.send(p, "&a가문 가입: " + target.name);
                }
                break;
            case "leave":
                plugin.clans().leave(p);
                Msg.send(p, "&7가문 탈퇴");
                break;
            case "info":
                Clan mine = plugin.clans().ofPlayer(p.getUniqueId());
                if (mine == null) { Msg.warn(p, "소속 가문 없음"); break; }
                Msg.send(p, "&6가문: " + mine.name + " (Lv " + mine.level + ")");
                Msg.send(p, "&7인원: " + mine.members.size() + "  보고: " + mine.treasury);
                break;
            case "lineage": {
                var pd = kr.reborn.core.RebornCore.get().api().getPlayerData(p.getUniqueId());
                if (pd == null) { Msg.warn(p, "데이터 없음"); break; }
                String ln = pd.lineage();
                if (ln == null || ln.isEmpty()) {
                    Msg.send(p, "&7혈통 없음 — 평범한 출신.");
                    break;
                }
                Msg.send(p, "&6&l[혈통] §f" + ln);
                String effect = lineageEffectDesc(ln);
                if (effect != null) Msg.send(p, "&7효과: " + effect);
                break;
            }
            case "list":
                Msg.send(p, "&6전체 가문:");
                plugin.clans().all().forEach(cn -> p.sendMessage("§e" + cn.id + " §7- " + cn.name + " (Lv " + cn.level + ")"));
                break;
            case "treasury":
                Clan g = plugin.clans().ofPlayer(p.getUniqueId());
                if (g != null) Msg.send(p, "&6금고: " + g.treasury);
                break;
            case "invite":
                if (a.length < 2) return true;
                Player tg = Bukkit.getPlayerExact(a[1]);
                if (tg == null) { Msg.error(p, "대상이 오프라인입니다."); break; }
                if (plugin.clans().invite(p, tg)) {
                    Clan myc = plugin.clans().ofPlayer(p.getUniqueId());
                    Msg.send(p, "&a초대 발송: " + tg.getName() + " (5분 유효)");
                    Msg.send(tg, "&d" + p.getName() + "이(가) §6" + (myc == null ? "?" : myc.name)
                            + " §d가문에 초대했다. §7수락: /clan join " + (myc == null ? "?" : myc.id));
                }
                break;
            case "war":
                if (a.length < 2) {
                    Msg.send(p, "&7/clan war declare <clanId> | join <clanId> | list");
                    return true;
                }
                String wsub = a[1].toLowerCase();
                if ("declare".equals(wsub) && a.length >= 3) {
                    plugin.wars().declareWar(p, a[2]);
                } else if ("join".equals(wsub) && a.length >= 3) {
                    plugin.wars().joinAsAlly(p, a[2]);
                } else if ("list".equals(wsub)) {
                    var active = plugin.wars().activeWars();
                    Msg.send(p, "&6=== 활성 가문 전쟁 (" + active.size() + ") ===");
                    for (var w : active) {
                        p.sendMessage("§c⚔ §f" + w.challengerClanId + " §c vs §f"
                                + w.defenderClanId + " §7점수 "
                                + (int) w.challengerScore + " : " + (int) w.defenderScore);
                    }
                }
                break;
            case "will":
                if (a.length < 2) {
                    var existing = plugin.inheritance().willOf(p.getUniqueId());
                    if (existing != null) {
                        Msg.send(p, "&6내 유언장 — 상속인: §f" + existing.heirId);
                    } else Msg.send(p, "&7/clan will <heirId> — 유언장 작성");
                    break;
                }
                plugin.inheritance().writeWill(p, a[1]);
                break;
        }
        return true;
    }

    private String lineageEffectDesc(String lineage) {
        return switch (lineage) {
            case "ROYAL" -> "&6왕족 — 카리스마 +30";
            case "ELF" -> "&a엘프 — 마나 +200, 매력 +20";
            case "DEMON_LORD" -> "&5마왕 — 마기 +500, 근력 +20";
            case "DEMON_NOBLE" -> "&5악마 귀족 — 마기 +200, 카리스마 +15";
            case "KITSUNE" -> "&d구미호 — 요기 +300, 매력 +40";
            case "GOLD_DRAGON" -> "&6드래곤 — 용력 +50, 브레스 자동 학습";
            case "SPIRIT" -> "&b정령 — 정령력 +200";
            case "MARTIAL_PURE" -> "&3무가 명문 — 내공 +300, 정신 +10";
            default -> null;
        };
    }
}

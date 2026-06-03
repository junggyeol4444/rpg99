package kr.reborn.core.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * ItemStack ↔ Base64 String — Bukkit 직렬화 기반.
 * NBT·메타·인챈트 모두 보존. 영구 저장(KV/DB)에 적합.
 */
public final class ItemSerializer {

    private ItemSerializer() {}

    public static String toBase64(ItemStack item) {
        if (item == null) return null;
        try (var baos = new ByteArrayOutputStream();
             var oos = new BukkitObjectOutputStream(baos)) {
            oos.writeObject(item);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Throwable t) { return null; }
    }

    public static ItemStack fromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        try (var bais = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             var ois = new BukkitObjectInputStream(bais)) {
            return (ItemStack) ois.readObject();
        } catch (Throwable t) { return null; }
    }
}

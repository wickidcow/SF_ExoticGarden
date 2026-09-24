package io.github.thebusybiscuit.exoticgarden;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.libraries.dough.common.ChatColors;

public final class CustomPotion extends ItemStack {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @ParametersAreNonnullByDefault
    public CustomPotion(String name, Color color, PotionEffect effect, String... lore) {
        super(Material.POTION);

        PotionMeta meta = (PotionMeta) getItemMeta();
        List<Component> list = new ArrayList<>();

        for (String line : lore) {
            list.add(LEGACY.deserialize(ChatColors.color(line)));
        }

        meta.displayName(LEGACY.deserialize(ChatColors.color(name)));
        meta.lore(list);
        meta.setColor(color);
        meta.addCustomEffect(effect, true);

        setItemMeta(meta);
    }

}
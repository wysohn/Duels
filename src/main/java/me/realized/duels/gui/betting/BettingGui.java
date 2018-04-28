package me.realized.duels.gui.betting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.cache.Setting;
import me.realized.duels.duel.DuelManager;
import me.realized.duels.gui.betting.buttons.DetailsButton;
import me.realized.duels.gui.betting.buttons.HeadButton;
import me.realized.duels.gui.betting.buttons.StateButton;
import me.realized.duels.util.gui.AbstractGui;
import me.realized.duels.util.gui.Button;
import me.realized.duels.util.inventory.InventoryBuilder;
import me.realized.duels.util.inventory.ItemBuilder;
import me.realized.duels.util.inventory.Slots;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BettingGui extends AbstractGui {

    private class Section {

        private final int start, end, height;

        Section(final int start, final int end, final int height) {
            this.start = start;
            this.end = end;
            this.height = height;
        }

        private boolean isPart(final int slot) {
            for (int y = 0; y < height; y++) {
                for (int x = start; x < end; x++) {
                    if (x + y * 9 == slot) {
                        return true;
                    }
                }
            }

            return false;
        }

        public List<ItemStack> collect() {
            final List<ItemStack> result = new ArrayList<>();
            Slots.doFor(start, end, height, slot -> result.add(inventory.getItem(slot)));
            return result;
        }
    }

    private final Section[] sections = {
        new Section(9, 13, 4),
        new Section(14, 18, 4)
    };

    private final DuelManager duelManager;
    private final Setting setting;
    private final Inventory inventory;
    private final UUID first, second;
    private boolean firstReady, secondReady;

    public BettingGui(final DuelsPlugin plugin, final Setting setting, final Player first, final Player second) {
        this.duelManager = plugin.getDuelManager();
        this.setting = setting;
        this.inventory = InventoryBuilder.of("Winner Takes All!", 54).build();
        this.first = first.getUniqueId();
        this.second = second.getUniqueId();
        Slots.doFor(13, 14, 5, slot -> inventory.setItem(slot, ItemBuilder.of(Material.IRON_FENCE).name(" ").build()));
        Slots.doFor(0, 3, slot -> inventory.setItem(slot, ItemBuilder.of(Material.STAINED_GLASS_PANE, 1, (short) 1).name(" ").build()));
        Slots.doFor(45, 48, slot -> inventory.setItem(slot, ItemBuilder.of(Material.STAINED_GLASS_PANE, 1, (short) 1).name(" ").build()));
        Slots.doFor(6, 9, slot -> inventory.setItem(slot, ItemBuilder.of(Material.STAINED_GLASS_PANE, 1, (short) 11).name(" ").build()));
        Slots.doFor(51, 54, slot -> inventory.setItem(slot, ItemBuilder.of(Material.STAINED_GLASS_PANE, 1, (short) 11).name(" ").build()));
        set(inventory, 48, new HeadButton(plugin, first));
        set(inventory, 50, new HeadButton(plugin, second));
        set(inventory, 3, new StateButton(plugin, this, first));
        set(inventory, 5, new StateButton(plugin, this, second));
        set(inventory, 4, new DetailsButton(plugin, setting));
    }

    private boolean isFirst(final Player player) {
        return player.getUniqueId().equals(first);
    }

    public Section getSection(final Player player) {
        return isFirst(player) ? sections[0] : sections[1];
    }

    private boolean isReady(final Player player) {
        return isFirst(player) ? firstReady : secondReady;
    }

    public void setReady(final Player player) {
        if (isFirst(player)) {
            firstReady = true;
        } else {
            secondReady = true;
        }

        if (firstReady && secondReady) {
            final Player first = Bukkit.getPlayer(this.first);
            first.closeInventory();
            final Player second = Bukkit.getPlayer(this.second);
            second.closeInventory();
            duelManager.startMatch(first, second, setting);
        }
    }

    public void update(final Player player, final Button button) {
        update(player, inventory, button);
    }

    @Override
    public void open(final Player player) {
        update(player);
        player.openInventory(inventory);
    }

    @Override
    public boolean isPart(final Inventory inventory) {
        return inventory.equals(this.inventory);
    }

    @Override
    public void on(final Player player, final Inventory top, final InventoryClickEvent event) {
        final Inventory clicked = event.getClickedInventory();

        if (clicked == null) {
            return;
        }

        final int slot = event.getSlot();

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            return;
        }

        if (!clicked.equals(top)) {
            return;
        }

        final Section section = getSection(player);

        if (section == null) {
            return;
        }

        if (!isReady(player) && section.isPart(slot)) {
            return;
        }

        event.setCancelled(true);

        final Optional<Button> cached = get(inventory, event.getSlot());

        if (!cached.isPresent()) {
            return;
        }

        cached.get().onClick(player);
    }

    @Override
    public void on(final Player player, final Set<Integer> rawSlots, final InventoryDragEvent event) {
        final Section section = getSection(player);

        if (section == null) {
            return;
        }

        boolean in = false;
        boolean out = false;
        boolean outSec = false;

        for (final int slot : rawSlots) {
            if (slot > 53) {
                out = true;
            } else {
                if (!section.isPart(slot)) {
                    outSec = true;
                }

                in = true;
            }
        }

        if (in && (isReady(player)|| out || outSec)) {
            event.setCancelled(true);
        }
    }
}

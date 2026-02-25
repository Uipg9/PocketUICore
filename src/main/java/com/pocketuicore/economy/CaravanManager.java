package com.pocketuicore.economy;

import com.pocketuicore.PocketUICore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CaravanManager — Server-side trade caravan dispatcher.
 * <p>
 * Players fund a caravan by consuming raw materials from their
 * inventory.  No entities are spawned — the trade is resolved
 * immediately.  If all required materials are present and consumed,
 * the player receives a bulk money payout and custom XP.
 *
 * <b>Usage:</b>
 * <pre>{@code
 *     CaravanResult result = CaravanManager.dispatchCaravan(player);
 *     if (result.success()) {
 *         // caravan dispatched — materials consumed, money + XP awarded
 *     } else {
 *         // insufficient materials
 *     }
 * }</pre>
 */
public final class CaravanManager {

    private CaravanManager() { /* utility class */ }

    // =====================================================================
    //  Configuration
    // =====================================================================

    /**
     * Materials required to fund a caravan trip.
     * Order: display-friendly (LinkedHashMap preserves insertion order).
     */
    private static final Map<Item, Integer> CARAVAN_COST = new LinkedHashMap<>();

    static {
        CARAVAN_COST.put(Items.OAK_LOG,   16);  // 16 logs
        CARAVAN_COST.put(Items.COBBLESTONE, 32); // 32 cobblestone
        CARAVAN_COST.put(Items.RAW_IRON,    4);  // 4 raw iron
        CARAVAN_COST.put(Items.BREAD,       8);  // 8 bread (provisions)
    }

    /** Money awarded on a successful trade route. */
    private static final int PAYOUT_AMOUNT = 200;

    /** Experience points awarded on a successful trade route. */
    private static final int XP_REWARD = 50;

    // =====================================================================
    //  Dispatch
    // =====================================================================

    /**
     * Attempt to dispatch a trade caravan for the given player.
     * <p>
     * Scans the player's inventory for every item in {@link #CARAVAN_COST}.
     * If <b>all</b> required materials are present, they are consumed
     * atomically, the player receives {@link #PAYOUT_AMOUNT} via
     * {@link EconomyManager#addMoney}, and {@link #XP_REWARD} experience
     * points are granted.
     *
     * @param player the server-side player attempting to dispatch
     * @return a {@link CaravanResult} indicating success/failure with details
     */
    public static CaravanResult dispatchCaravan(ServerPlayerEntity player) {
        // ── Phase 1: Verify all materials are available ──────────────────
        for (Map.Entry<Item, Integer> entry : CARAVAN_COST.entrySet()) {
            int available = countItem(player, entry.getKey());
            if (available < entry.getValue()) {
                PocketUICore.LOGGER.debug(
                        "[Caravan] {} missing materials: need {}x {} but has {}",
                        player.getName().getString(),
                        entry.getValue(),
                        entry.getKey().getName().getString(),
                        available);
                return new CaravanResult(false, 0, 0,
                        "Not enough " + entry.getKey().getName().getString()
                                + " (need " + entry.getValue()
                                + ", have " + available + ")");
            }
        }

        // ── Phase 2: Consume all materials atomically ────────────────────
        for (Map.Entry<Item, Integer> entry : CARAVAN_COST.entrySet()) {
            removeItem(player, entry.getKey(), entry.getValue());
        }

        // ── Phase 3: Grant rewards ──────────────────────────────────────
        EconomyManager.addMoney(player, PAYOUT_AMOUNT);
        player.addExperience(XP_REWARD);

        PocketUICore.LOGGER.info("[Caravan] {} dispatched a caravan → +${}, +{} XP",
                player.getName().getString(), PAYOUT_AMOUNT, XP_REWARD);

        return new CaravanResult(true, PAYOUT_AMOUNT, XP_REWARD, "Caravan dispatched!");
    }

    // =====================================================================
    //  Queries
    // =====================================================================

    /**
     * Get an unmodifiable view of the caravan material requirements.
     *
     * @return map of Item → required count
     */
    public static Map<Item, Integer> getCaravanCost() {
        return Map.copyOf(CARAVAN_COST);
    }

    /**
     * Check whether a player has all materials needed for a caravan,
     * without consuming anything.
     *
     * @param player the player to check
     * @return {@code true} if all materials are present in sufficient quantity
     */
    public static boolean canDispatch(ServerPlayerEntity player) {
        for (Map.Entry<Item, Integer> entry : CARAVAN_COST.entrySet()) {
            if (countItem(player, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    // =====================================================================
    //  Inventory Helpers
    // =====================================================================

    /**
     * Count how many of a given item the player has across their
     * entire inventory (main + offhand + armor).
     *
     * @param player the player
     * @param item   the item type to count
     * @return total count across all inventory slots
     */
    private static int countItem(ServerPlayerEntity player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Remove a specified quantity of an item from the player's
     * inventory, consuming across multiple stacks if needed.
     *
     * @param player the player
     * @param item   the item type to remove
     * @param amount total quantity to remove
     */
    private static void removeItem(ServerPlayerEntity player, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                int take = Math.min(remaining, stack.getCount());
                stack.decrement(take);
                remaining -= take;
            }
        }
    }

    // =====================================================================
    //  Result Record
    // =====================================================================

    /**
     * Immutable result of a caravan dispatch attempt.
     *
     * @param success    whether the caravan was dispatched
     * @param moneyEarned money deposited (0 if failed)
     * @param xpEarned    XP awarded (0 if failed)
     * @param message     human-readable status message
     */
    public record CaravanResult(boolean success, int moneyEarned,
                                int xpEarned, String message) {
    }
}

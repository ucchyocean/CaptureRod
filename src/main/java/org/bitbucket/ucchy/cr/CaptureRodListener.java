/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.cr;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;

/**
 * CaptureRodのListenerクラス
 * @author ucchy
 */
public class CaptureRodListener implements Listener {

    /**
     * ロッドの針を投げたときに呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onHook(PlayerFishEvent event) {

        final Player player = event.getPlayer();
        final FishHook hook = event.getHook();

        // パーミッションが無いなら何もしない
        if ( !player.hasPermission(CaptureRod.PERMISSION + "action") ) return;

        // 手に持っているアイテムがロッドでないなら、何もしない
        ItemStack rod = getItemInHand(player);
        if ( rod == null ||
                rod.getType() == Material.AIR ||
                !rod.getItemMeta().hasDisplayName() ||
                !rod.getItemMeta().getDisplayName().equals(CaptureRod.DISPLAY_NAME) ) {
            return;
        }

        if ( event.getState() == State.FISHING ) {
            // 針を投げるときの処理

            CaptureRodConfig config = CaptureRod.getInstance().getCaptureRodConfig();

            // 向いている方向のLivingEntityを取得する
            LivingEntity target = hookTargetLivingEntity(player, config.getCaptureRange());
            if ( target == null ) {
                player.sendMessage(ChatColor.RED + "target not found!!");
                event.setCancelled(true);
                return;
            }

            // 既にスタンしているなら、何もしない
            Entity passenger = target.getPassenger();
            if ( target.hasMetadata(CaptureRod.STAN_META_NAME) &&
                    passenger != null && passenger instanceof FishHook ) {
                player.sendMessage(ChatColor.RED + "already captured!!");
                event.setCancelled(true);
                return;
            }

            // 見つかったLivingEntityに針を載せる
            hook.teleport(target);
            target.setPassenger(hook);
            target.damage(0F, player);

            // 耐久度を消耗させる
            if ( config.getDurabilityCost() > 0 ) {
                short durability = rod.getDurability();
                durability += config.getDurabilityCost();
                if ( durability >= rod.getType().getMaxDurability() ) {
                    setItemInHand(player, new ItemStack(Material.AIR));
                    updateInventory(player);
                    player.getWorld().playSound(
                            player.getLocation(), SoundEnum.ITEM_BREAK.getBukkit(), 1, 1);
                } else {
                    rod.setDurability(durability);
                }
            }

            // 対象をスタンさせる
            StanEffectTask task = new StanEffectTask(CaptureRod.getInstance(), hook, target, player);
            task.startTask();

            // エフェクト
            hook.getWorld().playEffect(hook.getLocation(), Effect.POTION_BREAK, 21);
            hook.getWorld().playEffect(hook.getLocation(), Effect.POTION_BREAK, 21);

        }
    }

    /**
     * エンティティがダメージを受けたときのイベント
     * @param event
     */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {

        // 落下ダメージで、ダメージ保護用のメタデータを持っているなら、ダメージから保護する
        if ( event.getCause() == DamageCause.FALL &&
                event.getEntity().hasMetadata(CaptureRod.STAN_META_NAME) ) {
            event.setCancelled(true);
        }
    }

    /**
     * プレイヤーがクリックした時のイベント
     * @param event
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        // スタンしているプレイヤーは、何もできないようにする
        if ( event.getPlayer().hasMetadata(CaptureRod.STAN_META_NAME) ) {
            event.setCancelled(true);
            return;
        }
    }

    /**
     * エンティティがエンティティに攻撃した時のイベント
     * @param event
     */
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {

        // スタンしているプレイヤーは、攻撃ができないようにする
        if ( event.getDamager().hasMetadata(CaptureRod.STAN_META_NAME) ) {
            event.setCancelled(true);
            return;
        }
    }

    /**
     * プレイヤーが飛行モードに入るときのイベント
     * @param event
     */
    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {

        // スタンしているプレイヤーは、飛行モードに入れないようにする
        if ( event.getPlayer().hasMetadata(CaptureRod.STAN_META_NAME) ) {
            event.getPlayer().setFlying(false);
            if ( event.isFlying() ) {
                event.setCancelled(true);
            }
            return;
        }
    }

    /**
     * プレイヤーがサーバーを退出するときのイベント
     * @param event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        // メタデータが残っている場合は、削除しておく。
        if ( event.getPlayer().hasMetadata(CaptureRod.STAN_META_NAME) ) {
            event.getPlayer().removeMetadata(CaptureRod.STAN_META_NAME, CaptureRod.getInstance());
        }
    }

    /**
     * プレイヤーが手（メインハンド）に持っているアイテムを取得します。
     * @param player プレイヤー
     * @return 手に持っているアイテム
     */
    @SuppressWarnings("deprecation")
    private static ItemStack getItemInHand(Player player) {
        if ( Utility.isCB19orLater() ) {
            return player.getInventory().getItemInMainHand();
        } else {
            return player.getItemInHand();
        }
    }

    /**
     * 指定したプレイヤーの手に持っているアイテムを設定します。
     * @param player プレイヤー
     * @param item アイテム
     */
    @SuppressWarnings("deprecation")
    private static void setItemInHand(Player player, ItemStack item) {
        if ( Utility.isCB19orLater() ) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }
    }

    /**
     * プレイヤーのインベントリを更新する
     * @param player
     */
    @SuppressWarnings("deprecation")
    public static void updateInventory(Player player) {
        player.updateInventory();
    }

    /**
     * プレイヤーが向いている方向にあるLivingEntityを取得し、
     * 釣り針をそこに移動する。
     * @param player プレイヤー
     * @param size 取得する最大距離、140以上を指定しないこと
     * @return プレイヤーが向いている方向にあるLivingEntity。
     *         取得できない場合はnullがかえされる。
     */
    private static LivingEntity hookTargetLivingEntity(Player player, int range) {

        // ターゲット先周辺のエンティティを取得する
        Location center = player.getLocation().clone();
        double halfrange = (double)range / 2.0;
        center.add(center.getDirection().normalize().multiply(halfrange));
        ArrayList<LivingEntity> targets = new ArrayList<LivingEntity>();
        for ( Entity e : getNearbyEntities(center, halfrange) ) {
            if ( e instanceof LivingEntity && !player.equals(e) ) {
                targets.add((LivingEntity)e);
            }
        }

        // 視線の先にあるブロックを取得する
        BlockIterator it = new BlockIterator(player, range);

        while ( it.hasNext() ) {
            Block block = it.next();

            if ( block.getType() != Material.AIR ) {
                // ブロックが見つかった(遮られている)、処理を終わってnullを返す
                return null;

            } else {
                // 位置が一致するLivingEntityがないか探す
                for ( LivingEntity le : targets ) {
                    if ( block.getLocation().distanceSquared(le.getLocation()) <= 4.0 ) {
                        // 見つかったLivingEntityを返す
                        return le;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 指定した地点の周囲にいるエンティティを取得する
     * @param center 取得する中心点
     * @param distance 探索する距離
     * @return 取得したエンティティ
     */
    private static List<Entity> getNearbyEntities(Location center, double distance) {

        List<Entity> entities = new ArrayList<Entity>();
        double squared = distance * distance;
        for ( Entity e : center.getWorld().getEntities() ) {
            if ( center.distanceSquared(e.getLocation()) <= squared ) {
                entities.add(e);
            }
        }
        return entities;
    }
}

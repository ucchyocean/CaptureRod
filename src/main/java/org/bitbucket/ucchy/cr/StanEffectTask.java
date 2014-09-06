/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.cr;

import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.entity.Fish;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * 対象エンティティに、スタンエフェクトを与え続けるタスク
 * @author ucchy
 */
public class StanEffectTask extends BukkitRunnable {

    private CaptureRod plugin;
    private Fish hook;
    private LivingEntity le;
    private Player owner;
    private boolean running;

    /**
     * コンストラクタ
     * @param parent
     * @param hook
     * @param le
     * @param owner
     */
    public StanEffectTask(CaptureRod plugin, Fish hook, LivingEntity le, Player owner) {
        this.plugin = plugin;
        this.hook = hook;
        this.le = le;
        this.owner = owner;
        this.running = false;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        // フック、ターゲット、オーナー、いずれかが死んでいたらタスクを終了する
        if ( hook.isDead() || le.isDead() || owner.isDead() ) {
            endTask();
            return;
        }

        // エフェクトを与える
        applyEffect();

        // 釣り竿の方向にひっぱる
        setVelocityTowardOwner();
    }

    /**
     * タスクを開始する
     * @param plugin
     */
    protected void startTask() {

        if ( !running ) {
            runTaskTimer(plugin, 0, 3);
            running = true;
        }

        // 対象にメタデータを入れる
        le.setMetadata(CaptureRod.STAN_META_NAME,
                new FixedMetadataValue(plugin, true));
    }

    /**
     * タスクを終了する
     */
    protected void endTask() {

        cancel();
        running = false;
        if ( le != null && !le.isDead() ) {
            removeEffect();
            le.getWorld().playEffect(le.getLocation(), Effect.STEP_SOUND, 10);
            le.getWorld().playEffect(le.getLocation(), Effect.STEP_SOUND, 10);
        }
    }

    /**
     * タスクは動作中かどうかを返す
     * @return 動作中かどうか
     */
    protected boolean isRunning() {
        return running;
    }

    /**
     * 対象にスタンエフェクトを与える
     */
    private void applyEffect() {

        le.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW, 5, 10, true), true);
        le.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP, 5, -10, true), true);

        // 飛行を許可する
        if ( le instanceof Player ) {
            ((Player)le).setAllowFlight(true);
        }
    }

    /**
     * 対象からスタンエフェクトを除去する
     */
    private void removeEffect() {

        le.removePotionEffect(PotionEffectType.SLOW);
        le.removePotionEffect(PotionEffectType.JUMP);

        // メタデータを除去する
        le.removeMetadata(CaptureRod.STAN_META_NAME, plugin);
        le.setFallDistance(0);

        // 飛行を禁止する
        if ( le instanceof Player ) {
            Player player = (Player)le;
            if ( player.getGameMode() != GameMode.CREATIVE ) {
                player.setAllowFlight(false);
            }
        }
    }

    /**
     * 釣り竿の方向にひっぱる力を与える
     */
    private void setVelocityTowardOwner() {

        // 距離に応じて、飛び出す力を算出する
        double distance = owner.getEyeLocation().distance(le.getLocation());
        double wireLength = plugin.getCaptureRodConfig().getWireLength();
        double wireTension = plugin.getCaptureRodConfig().getWireTension();

        if ( distance > wireLength ) {

            double power = (distance - wireLength) / wireTension;

            // 飛び出す方向を算出する
            Vector vector = owner.getEyeLocation().subtract(le.getLocation())
                    .toVector().normalize().multiply(power);

            // 飛翔
            le.setVelocity(vector);
        }
    }
}

/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.sr;

import org.bukkit.Effect;
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

    private StanRod plugin;
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
    public StanEffectTask(StanRod plugin, Fish hook, LivingEntity le, Player owner) {
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

        // 対象に、落下ダメージ保護用のマークを入れる
        le.setMetadata(StanRod.PROTECT_FALL_META_NAME,
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
    }

    /**
     * 対象からスタンエフェクトを除去する
     */
    private void removeEffect() {

        le.removePotionEffect(PotionEffectType.SLOW);
        le.removePotionEffect(PotionEffectType.JUMP);

        // 落下保護を除去する
        le.removeMetadata(StanRod.PROTECT_FALL_META_NAME, plugin);
    }

    /**
     * 釣り竿の方向にひっぱる力を与える
     */
    private void setVelocityTowardOwner() {

        // 距離に応じて、飛び出す力を算出する
        double distance = owner.getEyeLocation().distance(le.getLocation());
        double power = 0;
        if ( distance > 3.0 ) {
            power = (distance - 3.0) / 5.0;
        }

        // 飛び出す方向を算出する
        Vector vector = owner.getEyeLocation().subtract(le.getLocation())
                .toVector().normalize().multiply(power);

        // 飛翔
        le.setVelocity(vector);
    }
}

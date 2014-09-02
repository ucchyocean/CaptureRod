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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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
    }

    /**
     * タスクを終了する
     */
    protected void endTask() {

        cancel();
        running = false;
        if ( le != null && !le.isDead() ) {
            removeEffect();
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

        // エフェクト
        if ( !hook.isDead() ) {
            hook.getWorld().playEffect(hook.getLocation(), Effect.STEP_SOUND, 10);
            hook.getWorld().playEffect(hook.getLocation(), Effect.STEP_SOUND, 10);
        }
    }
}

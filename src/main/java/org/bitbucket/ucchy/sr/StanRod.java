/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.sr;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.ComplexLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fish;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;

/**
 * スタンロッド
 * @author ucchy
 */
public class StanRod extends JavaPlugin implements Listener {

    private static final String NAME = "stanrod";
    private static final String DISPLAY_NAME = NAME;

    protected static final String PROTECT_FALL_META_NAME = "stanrodfallprotect";

    protected static StanRod instance;

    private ItemStack item;
    private StanRodConfig config;
    private ShapedRecipe recipe;

    /**
     * プラグインが有効になったときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable(){

        instance = this;

        config = new StanRodConfig(this);

        getServer().getPluginManager().registerEvents(this, this);

        item = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta wirerodMeta = item.getItemMeta();
        wirerodMeta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(wirerodMeta);

        if ( config.isEnableCraft() ) {
            makeRecipe();
        }

        // ColorTeaming のロード
        Plugin colorteaming = null;
        if ( getServer().getPluginManager().isPluginEnabled("ColorTeaming") ) {
            colorteaming = getServer().getPluginManager().getPlugin("ColorTeaming");
            String ctversion = colorteaming.getDescription().getVersion();
            if ( isUpperVersion(ctversion, "2.2.5") ) {
                getLogger().info("ColorTeaming was loaded. "
                        + getDescription().getName() + " is in cooperation with ColorTeaming.");
                ColorTeamingBridge bridge = new ColorTeamingBridge(colorteaming);
                bridge.registerItem(item, NAME, DISPLAY_NAME);
            } else {
                getLogger().warning("ColorTeaming was too old. The cooperation feature will be disabled.");
                getLogger().warning("NOTE: Please use ColorTeaming v2.2.5 or later version.");
            }
        }
    }

    /**
     * レシピを登録する
     */
    private void makeRecipe() {

        recipe = new ShapedRecipe(getStanRod());
        recipe.shape("  I", " IS", "I S");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STRING);
        getServer().addRecipe(recipe);
    }

    /**
     * レシピを削除する
     */
    private void removeRecipe() {

        Iterator<Recipe> it = getServer().recipeIterator();
        while ( it.hasNext() ) {
            Recipe recipe = it.next();
            ItemStack result = recipe.getResult();
            if ( !result.hasItemMeta() ||
                    !result.getItemMeta().hasDisplayName() ||
                    !result.getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
                continue;
            }
            it.remove();
        }

        this.recipe = null;
    }

    /**
     * コマンドが実行されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(
            CommandSender sender, Command command, String label, String[] args) {

        if ( args.length <= 0 ) {
            return false;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("stanrod.reload")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"stanrod.reload\".");
                return true;
            }

            // コンフィグ再読込
            config.reloadConfig();

            if ( recipe == null && config.isEnableCraft() ) {
                makeRecipe();
            } else if ( recipe != null && !config.isEnableCraft() ) {
                removeRecipe();
            }

            sender.sendMessage(ChatColor.GREEN + "StanRod configuration was reloaded!");

            return true;

        } else if (args[0].equalsIgnoreCase("get")) {

            if ( !(sender instanceof Player) ) {
                sender.sendMessage(ChatColor.RED + "This command can only use in game.");
                return true;
            }

            if (!sender.hasPermission("stanrod.get")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"stanrod.get\".");
                return true;
            }

            Player player = (Player)sender;
            giveWirerod(player);

            return true;

        } else if ( args.length >= 2 && args[0].equalsIgnoreCase("give") ) {

            if (!sender.hasPermission("stanrod.give")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"stanrod.give\".");
                return true;
            }

            Player player = getPlayer(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            giveWirerod(player);

            return true;
        }

        return false;
    }

    /**
     * StanRodを複製して取得する
     */
    private ItemStack getStanRod() {
        return this.item.clone();
    }

    /**
     * 指定したプレイヤーにStanRodを与える
     * @param player プレイヤー
     */
    private void giveWirerod(Player player) {

        ItemStack rod = getStanRod();
        ItemStack temp = player.getItemInHand();
        player.setItemInHand(rod);
        if ( temp != null ) {
            player.getInventory().addItem(temp);
        }
    }

    /**
     * StanRodの針を投げたり、針がかかったときに呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onHook(PlayerFishEvent event) {

        final Player player = event.getPlayer();
        final Fish hook = event.getHook();

        // パーミッションが無いなら何もしない
        if ( !player.hasPermission("stanrod.action") ) return;

        // 手に持っているアイテムがWireRodでないなら何もしない
        ItemStack rod = player.getItemInHand();
        if ( rod == null ||
                rod.getType() == Material.AIR ||
                !rod.getItemMeta().hasDisplayName() ||
                !rod.getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
            return;
        }

        if ( event.getState() == State.FISHING ) {
            // 針を投げるときの処理

            // 向いている方向のLivingEntityを取得し、その中にフックをワープさせる
            Entity target = hookTargetLivingEntity(player, hook, config.getWireRange());
            if ( target == null ) {
                player.sendMessage(ChatColor.RED + "target not found!!");
                event.setCancelled(true);
                return;
            }

            // 対象をスタンさせる
            LivingEntity le;
            if ( target instanceof LivingEntity ) {
                le = (LivingEntity)target;
            } else {
                le = (ComplexLivingEntity)((ComplexEntityPart)target).getParent();
            }
            StanEffectTask task = new StanEffectTask(this, hook, le, player);
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
                event.getEntity().hasMetadata(PROTECT_FALL_META_NAME) ) {
            event.setCancelled(true);
        }
    }

    /**
     * プレイヤーが向いている方向にあるLivingEntityを取得し、
     * 釣り針をそこに移動する。
     * @param player プレイヤー
     * @param hook 釣り針
     * @param size 取得する最大距離、140以上を指定しないこと
     * @return プレイヤーが向いている方向にあるEntityまたはComplexLivingEntity。
     * 取得できない場合はnullがかえされる
     */
    private static Entity hookTargetLivingEntity(Player player, Fish hook, int range) {

        // ターゲット先周辺のエンティティを取得する
        Location center = player.getLocation().clone();
        double halfrange = (double)range / 2.0;
        center.add(center.getDirection().multiply(halfrange));
        Entity orb = center.getWorld().spawnEntity(center, EntityType.EXPERIENCE_ORB);
        ArrayList<Entity> targets = new ArrayList<Entity>();
        for ( Entity e : orb.getNearbyEntities(halfrange, halfrange, halfrange) ) {
            if ( e instanceof LivingEntity && !player.equals(e) ) {
                targets.add(e);
            } else if ( e instanceof ComplexLivingEntity ) {
                for ( ComplexEntityPart part : ((ComplexLivingEntity)e).getParts() ) {
                    targets.add(part);
                }
            }
        }
        orb.remove();

        // 視線の先にあるブロックを取得する
        BlockIterator it = new BlockIterator(player, range);

        while ( it.hasNext() ) {
            Block block = it.next();

            if ( block.getType() != Material.AIR ) {
                // ブロックが見つかった(遮られている)、処理を終わってnullを返す
                return null;

            } else {
                // 位置が一致するLivingEntityがないか探す
                for ( Entity e : targets ) {
                    Location location = e.getLocation();
                    if ( block.getLocation().distanceSquared(e.getLocation()) <= 4.0 ) {
                        // LivingEntityが見つかった、針を載せる
                        hook.teleport(location);
                        e.setPassenger(hook);
                        if ( e instanceof LivingEntity ) {
                            ((LivingEntity)e).damage(0F, player);
                        } else if ( e instanceof ComplexEntityPart ) {
                            ((ComplexEntityPart)e).getParent().damage(0F, player);
                        }
                        // 見つかったLivingEntityを返す
                        return e;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 指定されたバージョンが、基準より新しいバージョンかどうかを確認する<br>
     * 完全一致した場合もtrueになることに注意。
     * @param version 確認するバージョン
     * @param border 基準のバージョン
     * @return 基準より確認対象の方が新しいバージョンかどうか
     */
    private boolean isUpperVersion(String version, String border) {

        String[] versionArray = version.split("\\.");
        int[] versionNumbers = new int[versionArray.length];
        for ( int i=0; i<versionArray.length; i++ ) {
            if ( !versionArray[i].matches("[0-9]+") )
                return false;
            versionNumbers[i] = Integer.parseInt(versionArray[i]);
        }

        String[] borderArray = border.split("\\.");
        int[] borderNumbers = new int[borderArray.length];
        for ( int i=0; i<borderArray.length; i++ ) {
            if ( !borderArray[i].matches("[0-9]+") )
                return false;
            borderNumbers[i] = Integer.parseInt(borderArray[i]);
        }

        int index = 0;
        while ( (versionNumbers.length > index) && (borderNumbers.length > index) ) {
            if ( versionNumbers[index] > borderNumbers[index] ) {
                return true;
            } else if ( versionNumbers[index] < borderNumbers[index] ) {
                return false;
            }
            index++;
        }
        if ( borderNumbers.length == index ) {
            return true;
        } else {
            return false;
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
     * 指定した名前のプレイヤーを取得する
     * @param name プレイヤー名
     * @return プレイヤー
     */
    @SuppressWarnings("deprecation")
    public static Player getPlayer(String name) {
        return Bukkit.getPlayerExact(name);
    }

    /**
     * このプラグインのjarファイルを返す
     * @return
     */
    protected File getJarFile() {
        return instance.getFile();
    }

}

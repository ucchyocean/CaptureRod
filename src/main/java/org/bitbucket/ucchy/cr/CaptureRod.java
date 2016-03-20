/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.cr;

import java.io.File;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * キャプチャロッド
 * @author ucchy
 */
public class CaptureRod extends JavaPlugin {

    private static final String NAME = "capturerod";
    protected static final String DISPLAY_NAME = NAME;
    protected static final String PERMISSION = NAME + ".";
    protected static final String STAN_META_NAME = "capturerodstan";

    private ItemStack item;
    private CaptureRodConfig config;
    private ShapedRecipe recipe;

    /**
     * プラグインが有効になったときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable() {

        // サーバーのバージョンが v1.7.10 以前なら、プラグインを停止して動作しない。
        if ( !Utility.isCB18orLater() ) {
            getLogger().warning("This plugin needs to run on Bukkit 1.8 or later version.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        config = new CaptureRodConfig(this);

        getServer().getPluginManager().registerEvents(new CaptureRodListener(), this);

        item = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta capturerodMeta = item.getItemMeta();
        capturerodMeta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(capturerodMeta);

        if ( config.isEnableCraft() ) {
            makeRecipe();
        }
    }

    /**
     * レシピを登録する
     */
    private void makeRecipe() {

        recipe = new ShapedRecipe(getRod());
        recipe.shape("  I", " IS", "I B");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STRING);
        recipe.setIngredient('B', Material.SLIME_BALL);
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

            if (!sender.hasPermission(PERMISSION + "reload")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"" + PERMISSION + "reload\".");
                return true;
            }

            // コンフィグ再読込
            config.reloadConfig();

            if ( recipe == null && config.isEnableCraft() ) {
                makeRecipe();
            } else if ( recipe != null && !config.isEnableCraft() ) {
                removeRecipe();
            }

            sender.sendMessage(ChatColor.GREEN + NAME + " configuration was reloaded!");

            return true;

        } else if (args[0].equalsIgnoreCase("get")) {

            if ( !(sender instanceof Player) ) {
                sender.sendMessage(ChatColor.RED + "This command can be used only in game.");
                return true;
            }

            if (!sender.hasPermission(PERMISSION + "get")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"" + PERMISSION + "get\".");
                return true;
            }

            Player player = (Player)sender;
            giveRod(player);

            return true;

        } else if ( args.length >= 2 && args[0].equalsIgnoreCase("give") ) {

            if (!sender.hasPermission(PERMISSION + "give")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"" + PERMISSION + "give\".");
                return true;
            }

            Player player = getPlayer(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            giveRod(player);

            return true;
        }

        return false;
    }

    /**
     * ロッドを複製して取得する
     */
    private ItemStack getRod() {
        return this.item.clone();
    }

    /**
     * 指定したプレイヤーにロッドを与える
     * @param player プレイヤー
     */
    private void giveRod(Player player) {

        ItemStack rod = getRod();
        ItemStack temp = getItemInHand(player);
        setItemInHand(player, rod);
        if ( temp != null && item.getType() != Material.AIR ) {
            player.getInventory().addItem(temp);
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
     * このプラグインのjarファイルを返す
     * @return
     */
    protected File getJarFile() {
        return getFile();
    }

    /**
     * このプラグインのコンフィグファイルを返す
     * @return コンフィグファイル
     */
    protected CaptureRodConfig getCaptureRodConfig() {
        return config;
    }

    /**
     * このプラグインのインスタンスを返す
     * @return インスタンス
     */
    protected static CaptureRod getInstance() {
        return (CaptureRod)Bukkit.getPluginManager().getPlugin("CaptureRod");
    }
}

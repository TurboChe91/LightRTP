package zf.TurboChe.LightRTP;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import io.github.cruciblemc.necrotempus.api.title.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin {
    private FileConfiguration config;
    private final Set<String> allowedWorlds = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private RTPManager rtpManager;
    private String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        reloadConfigSettings();

        //定义前缀
        this.prefix = ChatColor.BLUE + "[" + ChatColor.RED + "LightRTP" + ChatColor.BLUE + "] ";

        rtpManager = new RTPManager(this);
        Bukkit.getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "LightRTP 已启用");
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(prefix + ChatColor.RED + "LightRTP 已禁用");
    }

    public void reloadConfigSettings() {
        allowedWorlds.clear();
        allowedWorlds.addAll(config.getStringList("allowed-worlds"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "仅玩家可使用此命令！");
            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("reload") && player.hasPermission("lightrtp.reload")) {
            reloadConfig();
            reloadConfigSettings();
            player.sendMessage(ChatColor.GREEN + "配置已重载！");
            return true;
        }

        // 执行条件检查
        if (!checkConditions(player)) {
            return true;
        }
        rtpManager.startRTP(player);
        return true;
    }

    private boolean checkConditions(Player player) {
        // 世界检查
        if (!allowedWorlds.contains(player.getWorld().getName())) {
            String title = "§b§lInfinity";
            String subTitle = ChatColor.RED + "此世界不允许使用随机传送！";
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

            player.sendTitle(title, subTitle, 5, 15, 5);
            return false;
        }

        // OP不受冷却限制
        if (!player.isOp() && !player.hasPermission("lightrtp.unlimitedcd")) {
            long cooldown = getCooldownTime(player) * 1000; // 转为毫秒
            UUID uuid = player.getUniqueId();

            if (cooldowns.containsKey(uuid)) {
                long remaining = (cooldowns.get(uuid) + cooldown - System.currentTimeMillis()) / 1000;
                if (remaining > 0) {
                    String title = "§b§lInfinity";
                    String subTitle = ChatColor.RED + "请等待 " + remaining + " 秒后再使用！";
                    player.sendTitle(title, subTitle, 5, 15, 5);
                    return false;
                }
            }
        }

        // 权限检查
        if (!player.hasPermission("lightrtp.use") && !player.hasPermission("lightrtp.vip") && !player.isOp()) {

            player.sendMessage(ChatColor.RED + "你没有权限使用随机传送！");
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
            return false;
        }

        return true;
    }

    // 传送成功提示
    public void sendSuccessMessage(Player player, Location location) {
        int x = (int) location.getX();
        int y = (int) location.getY();
        int z = (int) location.getZ();
        String title = ChatColor.GREEN + "传送成功！";
        String subTitle = ChatColor.YELLOW + "你的坐标：" + x + ", " + y + ", " + z;
        player.sendTitle(title, subTitle, 5, 15, 5);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }

    // 根据玩家权限获取冷却时间（秒）
    public int getCooldownTime(Player player) {
        if (player.hasPermission("lightrtp.vip")) {
            return config.getInt("cooldown.vip", 60); // VIP冷却时间
        } else {
            return config.getInt("cooldown.default", 90); // 普通玩家冷却时间
        }
    }

    // 获取传送半径
    public int getRadius(Player player) {
        if (player.isOp() || player.hasPermission("lightrtp.vip")) {
            return config.getInt("radius.vip", 1000);
        } else {
            return config.getInt("radius.default", 700);
        }
    }

    public void setCooldown(Player player) {
        // 拥有特殊权限不设置冷却
        if (!player.hasPermission("lightrtp.unlimitedcd")) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }
}
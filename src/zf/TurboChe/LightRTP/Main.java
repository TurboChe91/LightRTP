package zf.TurboChe.LightRTP;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        reloadConfigSettings();

        rtpManager = new RTPManager(this);
        getLogger().info("LightRTP 已启用");
    }

    @Override
    public void onDisable() {
        getLogger().info("LightRTP 已禁用");
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

        if (!checkConditions(player)) return true;
        String title = "§b§lInfinity";
        String subTitle = ChatColor.GREEN + "正在寻找安全位置...";
        player.sendTitle(title, subTitle, 5, 15, 5);
        rtpManager.startRTP(player);
        return true;
    }

    private boolean checkConditions(Player player) {
        // 世界检查
        if (!allowedWorlds.contains(player.getWorld().getName())) {
            String title = "§b§lInfinity";
            String subTitle = ChatColor.RED + "此世界不允许使用随机传送！";

            player.sendTitle(title, subTitle, 5, 15, 5);
            return false;
        }

        // OP不受冷却限制
        if (!player.isOp() && !player.hasPermission("lightrtp.unlimitcd")) {
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

    // 其他Getter方法
    public Set<String> getDangerousBlocks() {
        return new HashSet<>(config.getStringList("dangerous-blocks"));
    }

    public int getMaxRetries() {
        return config.getInt("max-retries", 15);
    }

    public void setCooldown(Player player) {
        // 拥有特殊权限不设置冷却
        if (!player.hasPermission("lightrtp.unlimitcd")) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }
}
    
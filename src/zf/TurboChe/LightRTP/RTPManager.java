package zf.TurboChe.LightRTP;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import io.github.cruciblemc.necrotempus.api.title.*;

import java.util.Random;
import java.util.Set;

public class RTPManager {
    private final Main plugin;
    private final Random random = new Random();

    public RTPManager(Main plugin) {
        this.plugin = plugin;
    }

    public void startRTP(Player player) {
        final Player targetPlayer = player;
        
        // 异步寻找安全位置
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            World world = targetPlayer.getWorld();
            int radius = plugin.getRadius(targetPlayer);
            Set<String> dangerousBlocks = plugin.getDangerousBlocks();
            Location targetLocation = null;

            // 尝试寻找安全位置
            for (int i = 0; i < plugin.getMaxRetries(); i++) {
                targetLocation = generateRandomLocation(world, targetPlayer.getLocation(), radius);
                if (isLocationSafe(targetLocation, dangerousBlocks)) {
                    break;
                }
                targetLocation = null;
            }

            // 主线程执行传送
            final Location finalTarget = targetLocation;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalTarget == null) {
                    String title = ChatColor.RED + "多次尝试后仍未找到安全位置";
                    String subTitle = ChatColor.RED + "请重试!";

                    targetPlayer.sendTitle(title, subTitle, 20, 60, 20);
                    return;
                }

                // 启动倒计时
                startCountdown(targetPlayer, () -> {
                    targetPlayer.teleport(finalTarget);
                    plugin.sendSuccessMessage(targetPlayer, finalTarget);
                    plugin.setCooldown(targetPlayer);
                });
            });
        });
    }

    // 生成随机坐标
    private Location generateRandomLocation(World world, Location center, int radius) {
        int x = center.getBlockX() + random.nextInt(radius * 2) - radius;
        int z = center.getBlockZ() + random.nextInt(radius * 2) - radius;
        
        // 限制在世界边界内
        x = Math.max(-30000000, Math.min(x, 30000000));
        z = Math.max(-30000000, Math.min(z, 30000000));
        
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y, z + 0.5, center.getYaw(), center.getPitch());
    }

    // 安全检查
    private boolean isLocationSafe(Location loc, Set<String> dangerousBlocks) {
        if (loc.getY() < 1) return false;

        World world = loc.getWorld();
        Block foot = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        Block body = world.getBlockAt(loc);
        Block head = world.getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());

        String footType = foot.getType().name();
        String bodyType = body.getType().name();
        String headType = head.getType().name();

        if (dangerousBlocks.contains(footType) || dangerousBlocks.contains(bodyType) || dangerousBlocks.contains(headType)) {
            return false;
        }

        if (foot.isLiquid() || body.isLiquid() || head.isLiquid()) {
            return false;
        }

        return true;
    }

    // 倒计时提示（Title）
    private void startCountdown(Player player, Runnable onComplete) {
        final int totalSeconds = 3;
        final BukkitTask[] task = new BukkitTask[1];
        
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int remaining = totalSeconds - (task[0].getTaskId() % (totalSeconds + 1));

            if (remaining <= 0) {
                task[0].cancel();
                onComplete.run();
                return;
            }
            String title = ChatColor.GREEN +  "即将传送!";
            String subTitle = ChatColor.RED + "将在" + remaining + "秒后传送";

            player.sendTitle(title, subTitle, 5, 15, 5);
        }, 0, 20); // 每20ticks（1秒）执行一次
    }
}
    
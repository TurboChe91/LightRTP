package zf.TurboChe.LightRTP;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RTPManager {
    private final Main plugin;
    private final Random random = new Random();

    public RTPManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 启动随机传送流程
     * @param player 要传送的玩家
     */
    public void startRTP(Player player) {
        final Player targetPlayer = player;

        // 异步生成随机位置（避免阻塞主线程）
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            World world = targetPlayer.getWorld();
            int radius = plugin.getRadius(targetPlayer);
            Location targetLocation = null;

            // 生成随机位置
            targetLocation = generateRandomLocation(world, targetPlayer.getLocation(), radius);

            // 回到主线程执行传送相关操作
            final Location finalTarget = targetLocation;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalTarget == null) {
                    // 理论上不会触发（生成位置逻辑无重试限制）
                    String title = ChatColor.RED + "生成位置失败";
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

    /**
     * 生成随机坐标
     * @param world 世界
     * @param center 中心位置
     * @param radius 半径范围
     * @return 随机位置
     */
    private Location generateRandomLocation(World world, Location center, int radius) {
        // 在半径范围内生成随机X/Z坐标
        int x = center.getBlockX() + random.nextInt(radius * 2) - radius;
        int z = center.getBlockZ() + random.nextInt(radius * 2) - radius;

        // 限制在世界边界内
        x = Math.max(-30000000, Math.min(x, 30000000));
        z = Math.max(-30000000, Math.min(z, 30000000));

        // 获取该坐标的最高方块Y值
        int y = world.getHighestBlockYAt(x, z);
        // 调整坐标到方块中心，保留原方向
        return new Location(world, x + 0.5, y, z + 0.5, center.getYaw(), center.getPitch());
    }

    /**
     * 启动传送倒计时
     * @param player 玩家
     * @param onComplete 倒计时结束后执行传送
     */
    private void startCountdown(Player player, Runnable onComplete) {
        final int totalSeconds = 3;
        final BukkitTask[] task = new BukkitTask[1];
        final AtomicInteger secondsPassed = new AtomicInteger(0);

        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int passed = secondsPassed.get();
            int remaining = totalSeconds - passed;

            if (remaining <= 0) {
                task[0].cancel();
                onComplete.run();
                return;
            }

            // 发送标题提示
            String title = ChatColor.GREEN + "即将传送!";
            String subTitle = ChatColor.RED + "将在" + remaining + "秒后传送";
            player.sendTitle(title, subTitle, 5, 15, 5);

            // 播放倒计时音效
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

            secondsPassed.incrementAndGet();
        }, 0, 20);
    }
}
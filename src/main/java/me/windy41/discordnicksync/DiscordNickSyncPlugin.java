package me.windy41.discordnicksync;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordNickSyncPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("Essentials") == null) {
            getLogger().severe("EssentialsX not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (DiscordSRV.getPlugin() == null) {
            getLogger().severe("DiscordSRV is not loaded! This plugin requires DiscordSRV.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getLogger().severe("Found player " + player);
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId == null) {
            getLogger().severe("No linked Account!");
            return;
        }
        getLogger().severe("Found Discord ID of player: " + discordId);
        Guild guild = DiscordSRV.getPlugin().getJda().getGuildById("1117642319001305088");
        getLogger().severe("Found Server ID: " + guild);
        if (guild == null) {
            getLogger().severe("No Server ID in DiscordSRV config.yml");
            return;
        }
        guild.retrieveMemberById(discordId).queue((Member member) -> {
            String nickname = member.getNickname();
            getLogger().severe("Discord server name: " + nickname);
            if (nickname == null) {
                nickname = member.getUser().getName();
                getLogger().severe("No Nickname in server! Refering to Discord username");
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "nick " + player.getName() + " " + nickname);
            }   catch (Exception e) {
                getLogger().severe("Could not nickname player");
            }

        });
    }
}
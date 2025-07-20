package me.windy41.discordnicksync;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordNickSyncPlugin extends JavaPlugin implements Listener {

    private Essentials essentials;

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
            return;
        }

        this.essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getLogger().info("Found player: " + player.getName());

        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId == null) {
            getLogger().warning("No linked Discord account for " + player.getName());
            return;
        }

        Guild guild = DiscordSRV.getPlugin().getJda().getGuildById("1117642319001305088");
        if (guild == null) {
            getLogger().severe("Discord Guild ID not set correctly or not found.");
            return;
        }

        guild.retrieveMemberById(discordId).queue((Member member) -> {
            String nickname = member.getNickname();
            if (nickname == null || nickname.isBlank()) {
                nickname = member.getUser().getName();
            }

            DiscordNameParts parts = parseNickname(nickname);
            if (parts == null) {
                getLogger().warning("Could not parse nickname: " + nickname);
                return;
            }

            String formattedName = parts.name(); // e.g., "Owen W"

            // Set Essentials nickname and display names
            Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    User essentialsUser = essentials.getUser(player);
                    essentialsUser.setNickname(formattedName);
                } catch (Exception e) {
                    getLogger().severe("Failed to set Essentials nickname: " + e.getMessage());
                }

                player.setDisplayName(formattedName);
                player.setPlayerListName(formattedName);
            }, 20L); // delay 1 second to avoid plugin conflicts

            // Set prefix/suffix via LuckPerms
            LuckPerms luckPerms = LuckPermsProvider.get();
            UserManager userManager = luckPerms.getUserManager();

            userManager.loadUser(player.getUniqueId()).thenAccept(lpUser -> {
                lpUser.data().clear(NodeType.PREFIX::matches);
                lpUser.data().clear(NodeType.SUFFIX::matches);
                lpUser.data().clear(NodeType.INHERITANCE::matches); // clear old groups if needed

                PrefixNode prefixNode = PrefixNode.builder("[" + parts.house() + "] ", 10).build();
                SuffixNode suffixNode = SuffixNode.builder(" " + parts.initial() + ".", 10).build();
                lpUser.data().add(prefixNode);
                lpUser.data().add(suffixNode);

                // Assign house group (e.g., koru, britten)
                String houseGroup = parts.house().toLowerCase(); 
                lpUser.data().add(net.luckperms.api.node.types.InheritanceNode.builder(houseGroup).build());

                userManager.saveUser(lpUser);
            });
        });
    }

    private DiscordNameParts parseNickname(String nickname) {
        // Expected nickname format: "Owen W | Koru"
        Pattern pattern = Pattern.compile("^(.*?)\\s*\\|\\s*(.*)$");
        Matcher matcher = pattern.matcher(nickname);
        if (matcher.matches()) {
            String name = matcher.group(1).trim();   // e.g., "Owen W"
            String house = matcher.group(2).trim();  // e.g., "Koru"
            String[] nameParts = name.split("\\s+");
            String initial = nameParts.length > 1 ? nameParts[1].substring(0, 1) : "?";
            return new DiscordNameParts(house, name, initial);
        }
        return null;
    }

    private static class DiscordNameParts {
        private final String house;
        private final String name;
        private final String initial;

        public DiscordNameParts(String house, String name, String initial) {
            this.house = house;
            this.name = name;
            this.initial = initial;
        }

        public String house() {
            return house;
        }

        public String name() {
            return name;
        }

        public String initial() {
            return initial;
        }
    }
}
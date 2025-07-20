package me.windy41.discordnicksync;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;

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
            if (nickname == null) nickname = member.getUser().getName();

            DiscordNameParts parts = parseNickname(nickname);
            if (parts == null) {
                getLogger().warning("Could not parse nickname: " + nickname);
                return;
            }

            // Set EssentialsX nick
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "nick " + player.getName() + " " + parts.name());

            // Set display & tab name
            Bukkit.getScheduler().runTask(this, () -> {
                player.setDisplayName(parts.name());
                player.setPlayerListName(parts.name());
            });

            // Set prefix/suffix via LuckPerms
            LuckPerms luckPerms = LuckPermsProvider.get();
            luckPerms.getUserManager().loadUser(player.getUniqueId()).thenAccept(user -> {
                user.data().clear(NodeType.PREFIX::matches);
                user.data().clear(NodeType.SUFFIX::matches);

                PrefixNode prefixNode = PrefixNode.builder("[" + parts.house() + "] ", 10).build();
                SuffixNode suffixNode = SuffixNode.builder(parts.initial() + ".", 10).build();

                user.data().add(prefixNode);
                user.data().add(suffixNode);

                luckPerms.getUserManager().saveUser(user);
            });
        });
    }
    private DiscordNameParts parseNickname(String nickname) {
        // Example format: "[House] Name I." or "Name I."
        Pattern pattern = Pattern.compile("(.*?)\\s*\\|\\s*(.*)");
        Matcher matcher = pattern.matcher(nickname);
        if (matcher.matches()) {
            String name = matcher.group(1).trim();   // "Owen W"
            String house = matcher.group(2).trim();  // "Koru"
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
package me.uniodex.HydroHomie;

import me.uniodex.HydroHomie.data.ConfigManager;
import me.uniodex.HydroHomie.utils.Utils;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class HydroHomie extends JavaPlugin {

    private final List<UUID> notHydroHomies = new ArrayList<>();
    private List<String> messages;
    private String lastMessage;

    public String prefix;

    @Override
    public void onEnable() {
        loadPlugin(true);
    }

    @Override
    public void onDisable() {
        disablePlugin(true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("hydrohomie.reload")) {
                    reloadPlugin();
                    sender.sendMessage(getMessage("plugin-reload-message"));
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("sendmessage")) {
                if (sender.hasPermission("hydrohomie.sendmessage")) {
                    broadcastRandomMessage(false);
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("toggle")) {
                if (sender instanceof Player) {
                    if (sender.hasPermission("hydrohomie.toggle")) {
                        toggleMessages((Player) sender);
                        sender.sendMessage(getMessage("command-confirmation-message"));
                        return true;
                    }
                }
            }
        }

        sender.sendMessage(getMessage("invalid-command-message"));

        return true;
    }

    private void loadPlugin(boolean sendMessage) {
        new ConfigManager(this);

        setPrefix();

        messages = getConfig().getStringList("messages");

        List<String> notHydroHomiesList = getConfig().getStringList("notHydroHomies");
        for (String notHydroHomie : notHydroHomiesList) {
            notHydroHomies.add(UUID.fromString(notHydroHomie));
        }

        int sendMessageAfterXMinutes = ThreadLocalRandom.current().nextInt(getConfig().getInt("send-message-every-x-minutes-low"),
                getConfig().getInt("send-message-every-x-minutes-high") + 1);

        Bukkit.getScheduler().runTaskLater(this, () -> broadcastRandomMessage(true), sendMessageAfterXMinutes * 60 * 20);

        if (sendMessage)
            Bukkit.getLogger().log(Level.INFO, getMessage("plugin-enable-message"));
    }

    private void disablePlugin(boolean sendMessage) {
        Bukkit.getScheduler().cancelTasks(this);
        if (sendMessage)
            Bukkit.getLogger().log(Level.INFO, getMessage("plugin-disable-message"));
    }

    private void reloadPlugin() {
        disablePlugin(false);
        loadPlugin(false);
    }

    private String getMessage(String messagePath) {
        return Utils.GetColoredString(getConfig().getString(messagePath)).replace("%prefix%", prefix);
    }

    private void setPrefix() {
        prefix = Utils.GetColoredString(getConfig().getString("prefix"));

        if (getConfig().getBoolean("use-vault-prefix") && Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            Chat chat = rsp.getProvider();
            if (chat != null) {
                prefix = chat.getGroupPrefix((String) null, getConfig().getString("group-to-get-prefix-from"));
            }
        }
    }

    public void broadcastRandomMessage(boolean scheduleAnotherOne) {
        if (messages.size() == 0) {
            Bukkit.getLogger().log(Level.SEVERE, "You didn't set any messages homie. What do you expect me to say?");
            return;
        }

        String randomMessage;

        do {
            randomMessage = messages.get(ThreadLocalRandom.current().nextInt(0, messages.size()));
        }
        while (randomMessage.equals(lastMessage) && messages.size() > 1);

        lastMessage = randomMessage;

        Set<Player> players = new HashSet<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!notHydroHomies.contains(onlinePlayer.getUniqueId())) {
                players.add(onlinePlayer);
            }
        }

        for (Player player : players) {
            player.sendMessage(prefix + Utils.GetColoredString(randomMessage.replace("%playerName%", player.getDisplayName())));
        }

        if (scheduleAnotherOne) {
            int sendMessageAfterXMinutes = ThreadLocalRandom.current().nextInt(getConfig().getInt("send-message-every-x-minutes-low"),
                    getConfig().getInt("send-message-every-x-minutes-high") + 1);

            Bukkit.getScheduler().runTaskLater(this, () -> broadcastRandomMessage(true), sendMessageAfterXMinutes * 60 * 20);
        }
    }

    public void toggleMessages(Player player) {
        if (!notHydroHomies.remove(player.getUniqueId())) {
            notHydroHomies.add(player.getUniqueId());
        }

        List<String> notHydroHomiesStringList = new ArrayList<>();
        for (UUID notHydroHomie : notHydroHomies) {
            notHydroHomiesStringList.add(notHydroHomie.toString());
        }

        ConfigManager.instance.getConfig(ConfigManager.Config.DATA).set("notHydroHomies", notHydroHomiesStringList);
        ConfigManager.instance.saveConfig(ConfigManager.Config.DATA);
    }
}

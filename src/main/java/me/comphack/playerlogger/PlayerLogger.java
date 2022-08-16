package me.comphack.playerlogger;


import com.avaje.ebean.Update;
import me.comphack.playerlogger.commands.CommandManager;
import me.comphack.playerlogger.database.DatabaseManager;


import me.comphack.playerlogger.events.ChatEvent;
import me.comphack.playerlogger.events.JoinEvent;
import me.comphack.playerlogger.events.LeaveEvent;
import me.comphack.playerlogger.utils.Metrics;
import me.comphack.playerlogger.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;



public class PlayerLogger extends JavaPlugin implements Listener {
    private CommandManager cmd;
    private DatabaseManager dbmanager = new DatabaseManager();
    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        getLogger().info("Loaded Configurations");
        dbmanager.setupJDBC();
        dbmanager.PluginDatabase();
        getLogger().info("Loaded Database!");
        initializeEvents();
        cmd = new CommandManager(this);
        getLogger().info("Loaded Events & Commands");
        onEnableText();
        int pluginId = 16130;
        Metrics metrics = new Metrics(this, pluginId);
        getLogger().info("Checking for Updates...");
        if(getConfig().getBoolean("general.check-updates")) {
            UpdateChecker.init(this, 103033).requestUpdateCheck().whenComplete((result, exception) -> {
                if (result.requiresUpdate()) {
                    this.getLogger().info(String.format("An update is available! PlayerLogger %s may be downloaded on SpigotMC", result.getNewestVersion()));
                    return;
                }
                UpdateChecker.UpdateReason reason = result.getReason();
                if (reason == UpdateChecker.UpdateReason.UP_TO_DATE) {
                    this.getLogger().info(String.format("Your version of PlayerLogger (%s) is up to date!", result.getNewestVersion()));
                } else if (reason == UpdateChecker.UpdateReason.UNRELEASED_VERSION) {
                    this.getLogger().info(String.format("Your version of PlayerLogger (%s) is more recent than the one publicly available. Are you on a development build?", result.getNewestVersion()));
                } else {
                    this.getLogger().warning("Could not check for a new version of PlayerLogger. Reason: " + reason);
                }
            });
        } else {
            Bukkit.getLogger().info("Update Checking is disabled. You can enable it back through the config file.");
        }
    }

    public void onEnableText() {
        getLogger().info("--------------------------------------------------");
        getLogger().info("                                                  ");
        getLogger().info("          Enabled Player Logger                   ");
        getLogger().info("                 v1.1.0                       ");
        getLogger().info("                                                  ");
        getLogger().info("           Developed by COMPHACK                  ");
        getLogger().info("                                                  ");
        getLogger().info("--------------------------------------------------");


    }

    public void initializeEvents() {
        Bukkit.getServer().getPluginManager().registerEvents(new JoinEvent(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new LeaveEvent(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ChatEvent(), this);

    }




    @Override
    public void onDisable() {
        getLogger().info("PlayerLogger v1.0 has successfully shut down");
        getLogger().info("Thank You For using my plugin.");

    }
}

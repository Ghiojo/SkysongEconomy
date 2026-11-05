package org.skysongdev.skysongEconomy.data.database;

import org.bukkit.configuration.ConfigurationSection;

/** Connection settings, read from the {@code database} section of config.yml. */
public record DatabaseSettings(String host, String port, String user, String password, String databaseName) {

    public static DatabaseSettings fromConfig(ConfigurationSection section) {
        return new DatabaseSettings(
                section.getString("host", "localhost"),
                section.getString("port", "3306"),
                section.getString("user", "root"),
                section.getString("password", ""),
                section.getString("database_name", "skysong_economy"));
    }

    public String jdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName;
    }

    /** Keeps the password out of logs and stack traces. */
    @Override
    public String toString() {
        return "DatabaseSettings[" + user + "@" + host + ":" + port + "/" + databaseName + "]";
    }
}

package org.skysongdev.skysongEconomy.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/** Copies default files bundled in the plugin jar into the data folder without overwriting edits. */
public final class ResourceExtractor {
    private final JavaPlugin plugin;
    private final File jarFile;

    /** @param jarFile the plugin jar, from {@code JavaPlugin#getFile()} */
    public ResourceExtractor(JavaPlugin plugin, File jarFile) {
        this.plugin = plugin;
        this.jarFile = jarFile;
    }

    /** Copies every file under {@code folder/} in the jar that is not in the data folder yet. */
    public void extractFolder(String folder) {
        String prefix = folder + "/";
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String path = entry.getName();
                if (entry.isDirectory() || !path.startsWith(prefix)) {
                    continue;
                }
                if (!new File(plugin.getDataFolder(), path).exists()) {
                    plugin.saveResource(path, false);
                    plugin.getLogger().info("Created default file " + path);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to extract the default files in " + folder, e);
        }
    }
}

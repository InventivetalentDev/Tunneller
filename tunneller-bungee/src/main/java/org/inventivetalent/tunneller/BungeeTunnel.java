package org.inventivetalent.tunneller;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BungeeTunnel extends Plugin {

	private Configuration config;

	private AbstractTunnel tunnel;

	@SuppressWarnings("Duplicates")
	@Override
	public void onLoad() {
		saveDefaultConfig();

		this.tunnel = new AbstractTunnel(getLogger());

		config = getConfig();
		List<Map<String, String>> knownHostsList = (List<Map<String, String>>) config.getList("known_hosts");
		List identities = config.getList("identities");
		List<Map<String, Object>> tunnels = (List<Map<String, Object>>) config.getList("tunnels");
		this.tunnel.load(knownHostsList, identities, tunnels);
	}

	@Override
	public void onEnable() {
		List<Map<String, String>> knownHostList = new ArrayList<>();
		this.tunnel.enable(knownHostList);
		getConfig().set("known_hosts", knownHostList);
		saveConfig();
	}

	@Override
	public void onDisable() {
		this.tunnel.disable();
	}

	Configuration getConfig() {
		try {
			return ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void saveConfig() {
		try {
			ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(getDataFolder(), "config.yml"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void saveDefaultConfig() {
		if (!getDataFolder().exists()) { getDataFolder().mkdir(); }
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			try (InputStream in = getResourceAsStream("config.yml")) {
				Files.copy(in, file.toPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}

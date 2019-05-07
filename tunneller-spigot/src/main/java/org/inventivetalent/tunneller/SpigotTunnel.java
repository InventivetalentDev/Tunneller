package org.inventivetalent.tunneller;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpigotTunnel extends JavaPlugin {

	private AbstractTunnel tunnel;

	@SuppressWarnings("Duplicates")
	@Override
	public void onLoad() {
		saveDefaultConfig();

		this.tunnel = new AbstractTunnel(getLogger());

		FileConfiguration config = getConfig();
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
}

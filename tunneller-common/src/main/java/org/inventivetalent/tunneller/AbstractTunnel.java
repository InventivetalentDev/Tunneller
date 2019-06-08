package org.inventivetalent.tunneller;

import com.jcraft.jsch.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public  class AbstractTunnel {

	private Logger logger;

	protected JSch               jSch;
	protected Set<Session>       activeSessions = new HashSet<>();
	protected Map<String, String> knownHosts = new HashMap<>();

	protected AbstractTunnel() {
		this.jSch = new JSch();
	}

	protected AbstractTunnel(Logger logger) {
		this();
		this.logger=logger;
	}



	boolean load(List<Map<String,String>> knownHostsList,List identities,List<Map<String, Object>> tunnels) {
		getLogger().info("Setting known hosts...");
		for (Map<String, String> knownHost : knownHostsList) {
			knownHost.put((String) knownHost.get("host"), (String) knownHost.get("key"));

			try {
				jSch.getHostKeyRepository().add(new HostKey((String) knownHost.get("host"), Base64.getDecoder().decode((String) knownHost.get("key"))), null);
			} catch (JSchException e) {
				getLogger().log(Level.WARNING, "Failed to load host key", e);
			}
		}

		getLogger().info("Loading identities...");
		for (Object identity : identities) {
			if (identity instanceof String) {// only a file
				try {
					jSch.addIdentity((String) identity);
				} catch (JSchException e) {
					getLogger().log(Level.WARNING, "Failed to load identity " + ((String) identity), e);
				}
			} else if (identity instanceof Map) {// file + passphrase
				Map<String, String> map = (Map<String, String>) identity;
				if (!map.containsKey("file")) {
					getLogger().warning("Missing file for identity");
					continue;
				}
				if (!map.containsKey("passphrase")) {
					getLogger().warning("Missing passphrase for identity");
					continue;
				}
				try {
					jSch.addIdentity(map.get("file"), map.get("passphrase"));
				} catch (JSchException e) {
					getLogger().log(Level.WARNING, "Failed to load identity " + map.containsKey("file"), e);
				}
			} else {
				getLogger().warning("Invalid identity entry. Must be either a String or Map");
			}
		}
		try {
			getLogger().info("Identities loaded: "+jSch.getIdentityNames());
		} catch (JSchException ignored) {
		}

		getLogger().info("Creating tunnels...");
		for (Map<String, Object> tunnel : tunnels) {
			try {
				Session session = jSch.getSession((String) tunnel.get("username"), (String) tunnel.get("host"), (int) tunnel.getOrDefault("port", 22));
				if (tunnel.containsKey("password")) {
					session.setPassword((String) tunnel.get("password"));
				}
				if (tunnel.containsKey("key")) {
					session.setHostKeyAlias((String) tunnel.get("key"));
				}
				session.setUserInfo(new CommandLineUserInfo((String) tunnel.get("username"),(String) tunnel.get("host")));
				getLogger().info("  Connecting to " + (String) tunnel.get("username") + "@" + (String) tunnel.get("host") + "...");
				session.connect((int) tunnel.getOrDefault("timeout", 5000));
				session.setPortForwardingL((int) tunnel.get("localPort"), (String) tunnel.get("remoteHost"), (int) tunnel.get("remotePort"));
				getLogger().info("  Tunnel created :" + (int) tunnel.get("localPort") + " => " + (String) tunnel.get("remoteHost") + ":" + (int) tunnel.get("remotePort"));

				activeSessions.add(session);
			} catch (JSchException e) {
				getLogger().log(Level.WARNING, "Failed to create session " + tunnel.get("username") + "@" + tunnel.get("host"), e);
			}
		}

		return true;
	}

	boolean enable(List<Map<String, String>> knownHostList) {
		getLogger().info("There are " + activeSessions.size() + " active sessions");

		for (HostKey key : jSch.getHostKeyRepository().getHostKey()) {
			knownHosts.put(key.getHost(), key.getKey());

			Map<String, String> knownHost = new HashMap<>();
			knownHost.put("host", key.getHost());
			knownHost.put("key", key.getKey());
			knownHostList.add(knownHost);
		}
		// the knownHost list is meant to be initialized by the caller and then saved back to the config
		return true;
	}

	boolean disable() {
		getLogger().info("Disconnecting sessions...");
		for (Session session : activeSessions) {
			try {
				session.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	Logger getLogger() {
		if (this.logger == null) {
			this.logger = Logger.getLogger("Tunneller");
		}
		return this.logger;
	}
	

	static class CommandLineUserInfo implements UserInfo {

		String username;
		String host;

		public CommandLineUserInfo(String username, String host) {
			this.username = username;
			this.host = host;
		}

		@Override
		public String getPassphrase() {
			return null;
		}

		@Override
		public String getPassword() {
			return null;
		}

		@Override
		public boolean promptPassword(String s) {
			return false;
		}

		@Override
		public boolean promptPassphrase(String s) {
			return false;
		}

		@Override
		public boolean promptYesNo(String s) {
			System.out.println("[SSH] ("+username+"@"+host+") " + s);
			Scanner scanner = new Scanner(System.in);
			return "yes".equalsIgnoreCase(scanner.nextLine());
		}

		@Override
		public void showMessage(String s) {
			System.out.println("[SSH] ("+username+"@"+host+") " + s);
		}
	}

}

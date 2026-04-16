package com.javify.dao;

import com.javify.services.PlayerService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// create and read/write app state to a JSON file in the db directory
public class AppStateDAO {

	private static final Path STATE_FILE = Paths.get("db", "app-state.json");

	public void setSetting(String key, String value) {
		StateData data = readState();
		data.settings.put(key, value);
		writeState(data);
	}

	public String getSetting(String key) {
		return readState().settings.get(key);
	}

	public void setLastUserId(int userId) {
		StateData data = readState();
		data.lastUserId = userId;
		writeState(data);
	}

	public Integer getLastUserId() {
		return readState().lastUserId;
	}

	public void clearLastUserId() {
		StateData data = readState();
		data.lastUserId = null;
		writeState(data);
	}

	// save playback state for a specific user
	public void savePlaybackState(int userId, Integer trackId, long positionMicroseconds, PlayerService.State state) {
		StateData data = readState();
		data.playbackByUser.put(userId, new PlaybackState(
				trackId,
				Math.max(0L, positionMicroseconds),
				state != null ? state : PlayerService.State.STOPPED
		));
		writeState(data);
	}

	public PlaybackState loadPlaybackState(int userId) {
		return readState().playbackByUser.get(userId);
	}

	// read app state
	private StateData readState() {
		try {
			ensureParentDirectory();
			if (!Files.exists(STATE_FILE)) {
				return new StateData();
			}

			String json = Files.readString(STATE_FILE, StandardCharsets.UTF_8);
			if (json == null || json.isBlank()) {
				return new StateData();
			}
			return parseJson(json);
		} catch (Exception e) {
			// if state is corrupted, continue with defaults
			return new StateData();
		}
	}

	// write app state
	private void writeState(StateData data) {
		try {
			ensureParentDirectory();
			Files.writeString(STATE_FILE, toJson(data), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to write app state file.", e);
		}
	}

	private void ensureParentDirectory() throws IOException {
		Path parent = STATE_FILE.getParent();
		if (parent != null && !Files.exists(parent)) {
			Files.createDirectories(parent);
		}
	}

	// parse JSON string (thanks AI)
	private StateData parseJson(String json) {
		StateData data = new StateData();

		Matcher lastUserMatcher = Pattern.compile("\"lastUserId\"\\s*:\\s*(null|-?\\d+)").matcher(json);
		if (lastUserMatcher.find()) {
			String value = lastUserMatcher.group(1);
			if (!"null".equals(value)) {
				data.lastUserId = Integer.parseInt(value);
			}
		}

		String settingsObject = extractObject(json, "settings");
		if (settingsObject != null) {
			Matcher entry = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(settingsObject);
			while (entry.find()) {
				data.settings.put(unescape(entry.group(1)), unescape(entry.group(2)));
			}
		}

		String playbackObject = extractObject(json, "playbackByUser");
		if (playbackObject != null) {
			Matcher userEntry = Pattern.compile("\"(\\d+)\"\\s*:\\s*\\{([^}]*)}").matcher(playbackObject);
			while (userEntry.find()) {
				int userId = Integer.parseInt(userEntry.group(1));
				String body = userEntry.group(2);

				Integer trackId = null;
				Matcher trackMatcher = Pattern.compile("\"trackId\"\\s*:\\s*(null|-?\\d+)").matcher(body);
				if (trackMatcher.find()) {
					String value = trackMatcher.group(1);
					if (!"null".equals(value)) {
						trackId = Integer.parseInt(value);
					}
				}

				long position = 0L;
				Matcher posMatcher = Pattern.compile("\"positionMicroseconds\"\\s*:\\s*(-?\\d+)").matcher(body);
				if (posMatcher.find()) {
					position = Math.max(0L, Long.parseLong(posMatcher.group(1)));
				}

				PlayerService.State state = PlayerService.State.STOPPED;
				Matcher stateMatcher = Pattern.compile("\"state\"\\s*:\\s*\"([A-Z_]+)\"").matcher(body);
				if (stateMatcher.find()) {
					try {
						state = PlayerService.State.valueOf(stateMatcher.group(1));
					} catch (IllegalArgumentException ignored) {
						state = PlayerService.State.STOPPED;
					}
				}

				data.playbackByUser.put(userId, new PlaybackState(trackId, position, state));
			}
		}

		return data;
	}

	// get a specific object from a JSON string
	private String extractObject(String json, String key) {
		int keyIndex = json.indexOf("\"" + key + "\"");
		if (keyIndex < 0) {
			return null;
		}

		int start = json.indexOf('{', keyIndex);
		if (start < 0) {
			return null;
		}

		int depth = 0;
		for (int i = start; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0) {
					return json.substring(start + 1, i);
				}
			}
		}
		return null;
	}

	// convert app state to JSON string
	private String toJson(StateData data) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"lastUserId\": ").append(data.lastUserId == null ? "null" : data.lastUserId).append(",\n");

		sb.append("  \"settings\": {\n");
		int i = 0;
		for (Map.Entry<String, String> entry : data.settings.entrySet()) {
			sb.append("    \"").append(escape(entry.getKey())).append("\": \"")
					.append(escape(entry.getValue())).append("\"");
			if (++i < data.settings.size()) {
				sb.append(',');
			}
			sb.append('\n');
		}
		sb.append("  },\n");

		sb.append("  \"playbackByUser\": {\n");
		int p = 0;
		for (Map.Entry<Integer, PlaybackState> entry : data.playbackByUser.entrySet()) {
			PlaybackState state = entry.getValue();
			sb.append("    \"").append(entry.getKey()).append("\": { ")
					.append("\"trackId\": ").append(state.trackId() == null ? "null" : state.trackId()).append(", ")
					.append("\"positionMicroseconds\": ").append(Math.max(0L, state.positionMicroseconds())).append(", ")
					.append("\"state\": \"").append(state.state().name()).append("\" }");
			if (++p < data.playbackByUser.size()) {
				sb.append(',');
			}
			sb.append('\n');
		}
		sb.append("  }\n");
		sb.append("}\n");

		return sb.toString();
	}

	private String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private String unescape(String value) {
		return value.replace("\\\"", "\"").replace("\\\\", "\\");
	}

	private static class StateData {
		private Integer lastUserId;
		private final Map<String, String> settings = new LinkedHashMap<>();
		private final Map<Integer, PlaybackState> playbackByUser = new LinkedHashMap<>();
	}

	public record PlaybackState(Integer trackId, long positionMicroseconds, PlayerService.State state) {
	}
}


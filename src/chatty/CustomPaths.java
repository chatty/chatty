
package chatty;

import chatty.Chatty.PathType;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tduva
 */
public class CustomPaths {
    
    private final Map<Chatty.PathType, ChattyPath> paths = new HashMap<>();
    private Settings settings;

    public synchronized Path getPath(Chatty.PathType type) {
        return get(type).get();
    }

    public synchronized Path getPathAndCreate(Chatty.PathType type) {
        return get(type).getAndCreate();
    }

    public synchronized ChattyPath get(Chatty.PathType type) {
        if (!paths.containsKey(type)) {
            ChattyPath p = new ChattyPath();
            p.defaultDir = type.createDefault.get();
            paths.put(type, p);
        }
        return paths.get(type);
    }

    public synchronized void setSettings(Settings settings) {
        this.settings = settings;
        for (Chatty.PathType type : Chatty.PathType.values()) {
            if (type.settingName != null) {
                loadFromSettings(type);
            }
        }
    }

    private synchronized boolean loadFromSettings(Chatty.PathType type) {
        if (settings == null || type.settingName == null) {
            return false;
        }
        String value = settings.getString(type.settingName);
        if (value.isEmpty()) {
            return false;
        }
        try {
            Path path = Paths.get(value);
            setCustom(type, path, "setting " + type.settingName, true);
            return true;
        }
        catch (InvalidPathException ex) {
            ChattyPath p = get(type);
            p.customDir = null;
            p.invalidCustomDir = value;
            p.customInfo = "setting " + type.settingName;
            return false;
        }
    }
    
    public void updateFromSettings(PathType type) {
        loadFromSettings(type);
    }

    public synchronized void setCustom(Chatty.PathType type, Path path, String info, boolean requireExists) {
        ChattyPath p = get(type);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), path.toString());
        }
        if (requireExists && !Files.isDirectory(path)) {
            p.customDir = null;
            p.invalidCustomDir = path.toString();
            p.customInfo = info;
        }
        else {
            p.customDir = path;
            p.invalidCustomDir = null;
            p.customInfo = info;
        }
    }

    public void setCustom(Chatty.PathType type, String path, String info, boolean requireExists) {
        setCustom(type, Paths.get(path), info, requireExists);
    }
    
    public synchronized String getInfo(PathType type) {
        ChattyPath p = get(type);
        String customInfo = p.getCustomInfo() != null ? " (via "+p.getCustomInfo()+")" : "";
        if (p.getInvalid() != null) {
            return String.format("'%s', invalid custom path not used: '%s'%s",
                p.get(),
                p.getInvalid(),
                customInfo);
        }
        return String.format("'%s'%s%s",
                p.get(),
                customInfo,
                p.getOrig() != null ? " (default: '"+p.getOrig()+"')" : "");
    }

    public synchronized String getDebugInfo() {
        StringBuilder b = new StringBuilder();
        for (Chatty.PathType type : paths.keySet()) {
            ChattyPath path = paths.get(type);
            String info = "";
            if (path.getInvalid() != null) {
                info = StringUtil.append(info, " ",
                        String.format("[Invalid] %s",
                                path.getInvalid()));
            }
            if (path.getOrig() != null) {
                info = StringUtil.append(info, " ",
                        String.format("%s (%s)",
                                path.customDir, path.getOrig()));
            }
            if (path.getCustomInfo() != null) {
                info = StringUtil.append(info, " ",
                        String.format("via %s",
                                path.getCustomInfo()));
            }
            if (!info.isEmpty()) {
                b.append(String.format("[%s] %s\n", type, info));
            }
        }
        if (b.length() > 0) {
            b.insert(0, "[Custom Paths]\n");
        }
        return b.toString();
    }

    public synchronized String getInvalidInfo() {
        StringBuilder b = new StringBuilder();
        for (Chatty.PathType type : paths.keySet()) {
            ChattyPath path = paths.get(type);
            String info = "";
            if (path.getInvalid() != null) {
                b.append(StringUtil.append(info, " ",
                        String.format("[%s] %s (%s)\n",
                                type, path.getInvalid(), path.getCustomInfo())));
            }
        }
        if (b.length() > 0) {
            b.insert(0, "Invalid custom paths:\n");
        }
        return b.toString();
    }

    public static class ChattyPath {

        private Path defaultDir;
        private String invalidCustomDir;
        private Path customDir;
        private String customInfo;

        public Path get() {
            if (customDir != null) {
                return customDir;
            }
            return defaultDir;
        }

        public Path getAndCreate() {
            Path path = get();
            path.toFile().mkdirs();
            return path;
        }

        public String getInvalid() {
            return invalidCustomDir;
        }

        public Path getOrig() {
            return customDir != null ? defaultDir : null;
        }

        public String getCustomInfo() {
            return customInfo;
        }

        public Path getDefault() {
            return defaultDir;
        }

    }

}

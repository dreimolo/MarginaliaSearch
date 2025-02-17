package nu.marginalia.process.log;

import org.apache.logging.log4j.util.Strings;

import java.util.regex.Pattern;

public record WorkLogEntry(String id, String ts, String path, int cnt) {
    private static final Pattern splitPattern = Pattern.compile("\\s+");

    static WorkLogEntry parse(String line) {
        String[] parts = splitPattern.split(line);
        return new WorkLogEntry(parts[0], parts[1], parts[2], Integer.parseInt(parts[3]));
    }

    static boolean isJobId(String line) {
        return Strings.isNotBlank(line) && !line.startsWith("#");
    }

    static String parseJobIdFromLogLine(String s) {
        return splitPattern.split(s, 2)[0];
    }

    public String fileName() {
        if (path.contains("/")) {
            return path.substring(path.lastIndexOf("/") + 1);
        }
        return path;
    }

    public String relPath() {
        // Compatibility trick!

        String relPath = fileName();

        return relPath.substring(0, 2) + "/" + relPath.substring(2, 4) + "/" + relPath;
    }
}

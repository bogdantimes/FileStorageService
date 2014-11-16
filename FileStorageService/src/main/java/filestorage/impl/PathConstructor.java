package filestorage.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.separator;

/**
 * Maximum 2047 files in the lowest level folder.
 *
 * @author Bogdan Kovalev.
 */
public class PathConstructor {

    private static final List<Integer> dividers = new ArrayList<Integer>() {{
        add(Math.abs(Integer.MIN_VALUE / 64));
        add(get(0) / 128);
        add(get(1) / 128);
    }};

    public static String constructFilePathInStorage(String key, String rootFolder) {
        final int hashcode = key.hashCode();

        String path = "";

        for (int div : dividers) {
            long left_boundary = hashcode - hashcode % div;
            long right_boundary = left_boundary + div - 1;
            path += separator + createName(left_boundary, right_boundary);
        }

        try {
            Files.createDirectories(new File(path).toPath());
        } catch (IOException e) {
            //TODO failed to create directories for file
            e.printStackTrace();
        }

        return path.concat(separator).concat(key);
    }

    private static String createName(long left_boundary, long right_boundary) {
        return "[" + left_boundary + "_" + right_boundary + "]";
    }
}
package com.callibrity.cowork.connector.tools;

import com.callibrity.mocapi.tools.annotation.Tool;
import com.callibrity.mocapi.tools.annotation.ToolService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Component
@ToolService
public class FileSystemTool {

    @Tool(name = "fs.list-directory", description = "Lists files and directories at the given path")
    public ListDirectoryResponse listDirectory(
            @Schema(description = "Absolute path to the directory to list") String path) throws IOException {
        Path dir = Path.of(path);
        if (!Files.isDirectory(dir)) {
            return new ListDirectoryResponse(path, List.of(), "Not a directory: " + path);
        }
        try (Stream<Path> entries = Files.list(dir)) {
            List<FileEntry> files = entries
                    .map(p -> new FileEntry(
                            p.getFileName().toString(),
                            Files.isDirectory(p) ? "directory" : "file",
                            silentSize(p)))
                    .sorted((a, b) -> a.type().compareTo(b.type()) != 0
                            ? a.type().compareTo(b.type())
                            : a.name().compareTo(b.name()))
                    .toList();
            return new ListDirectoryResponse(path, files, null);
        }
    }

    @Tool(name = "fs.search-files", description = "Recursively searches for files matching a name pattern under a directory")
    public SearchFilesResponse searchFiles(
            @Schema(description = "Absolute path to the root directory to search") String directory,
            @Schema(description = "Glob pattern to match filenames against, e.g. *.java or *.{yml,yaml}") String pattern) throws IOException {
        Path root = Path.of(directory);
        if (!Files.isDirectory(root)) {
            return new SearchFilesResponse(directory, pattern, List.of(), "Not a directory: " + directory);
        }
        try (Stream<Path> walk = Files.walk(root)) {
            List<String> matches = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> root.getFileSystem().getPathMatcher("glob:" + pattern).matches(p.getFileName()))
                    .map(p -> root.relativize(p).toString())
                    .sorted()
                    .toList();
            return new SearchFilesResponse(directory, pattern, matches, null);
        }
    }

    private long silentSize(Path p) {
        try {
            return Files.isRegularFile(p) ? Files.size(p) : -1L;
        } catch (IOException e) {
            return -1L;
        }
    }

    public record FileEntry(
            @Schema(description = "File or directory name") String name,
            @Schema(description = "Entry type: 'file' or 'directory'") String type,
            @Schema(description = "File size in bytes, or -1 for directories") long sizeBytes) {
    }

    public record ListDirectoryResponse(
            @Schema(description = "The path that was listed") String path,
            @Schema(description = "Entries found in the directory") List<FileEntry> entries,
            @Schema(description = "Error message, if any") String error) {
    }

    public record SearchFilesResponse(
            @Schema(description = "The root directory searched") String directory,
            @Schema(description = "The pattern used") String pattern,
            @Schema(description = "Relative paths of matched files") List<String> matches,
            @Schema(description = "Error message, if any") String error) {
    }
}

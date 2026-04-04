package com.callibrity.cowork.connector.tools;

import com.callibrity.mocapi.tools.annotation.Tool;
import com.callibrity.mocapi.tools.annotation.ToolService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@ToolService
public class ShellCommandTool {

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "date", "df", "echo", "hostname", "ls", "pwd", "uname", "uptime", "whoami"
    );

    private static final int TIMEOUT_SECONDS = 10;

    @Tool(name = "shell.run", description = "Runs a safe, read-only shell command and returns its output. " +
            "Allowed commands: date, df, echo, hostname, ls, pwd, uname, uptime, whoami.")
    public ShellCommandResponse runCommand(
            @Schema(description = "The command to run, including any arguments. Only the allowed commands listed above are permitted.") String command) {
        List<String> tokens = Arrays.stream(command.strip().split("\\s+"))
                .filter(t -> !t.isBlank())
                .collect(Collectors.toList());

        if (tokens.isEmpty()) {
            return new ShellCommandResponse(command, "", "No command provided", -1);
        }

        String executable = tokens.getFirst();
        if (!ALLOWED_COMMANDS.contains(executable)) {
            return new ShellCommandResponse(command, "",
                    "Command not allowed: '" + executable + "'. Allowed commands: " + ALLOWED_COMMANDS, -1);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(tokens);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ShellCommandResponse(command, output, "Command timed out after " + TIMEOUT_SECONDS + " seconds", -1);
            }

            return new ShellCommandResponse(command, output, null, process.exitValue());
        } catch (Exception e) {
            return new ShellCommandResponse(command, "", "Failed to execute command: " + e.getMessage(), -1);
        }
    }

    public record ShellCommandResponse(
            @Schema(description = "The command that was run") String command,
            @Schema(description = "Standard output of the command") String output,
            @Schema(description = "Error message, if any") String error,
            @Schema(description = "Exit code of the process, or -1 if the command did not complete") int exitCode) {
    }
}

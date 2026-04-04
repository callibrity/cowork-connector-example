package com.callibrity.cowork.connector.tools;

import com.callibrity.mocapi.tools.annotation.Tool;
import com.callibrity.mocapi.tools.annotation.ToolService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@ToolService
public class SystemInfoTool {

    @Tool(name = "system.info", description = "Returns information about the host system: OS, hostname, CPU, memory, and disk usage")
    public SystemInfoResponse getSystemInfo() throws UnknownHostException {
        Runtime runtime = Runtime.getRuntime();
        File root = new File("/");

        return new SystemInfoResponse(
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                resolveHostname(),
                runtime.availableProcessors(),
                runtime.totalMemory(),
                runtime.freeMemory(),
                root.getTotalSpace(),
                root.getFreeSpace()
        );
    }

    private String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    public record SystemInfoResponse(
            @Schema(description = "Operating system name") String osName,
            @Schema(description = "Operating system version") String osVersion,
            @Schema(description = "CPU architecture") String osArch,
            @Schema(description = "Hostname of the machine") String hostname,
            @Schema(description = "Number of available CPU processors") int availableProcessors,
            @Schema(description = "Total JVM heap memory in bytes") long totalMemoryBytes,
            @Schema(description = "Free JVM heap memory in bytes") long freeMemoryBytes,
            @Schema(description = "Total disk space on the root filesystem in bytes") long totalDiskBytes,
            @Schema(description = "Free disk space on the root filesystem in bytes") long freeDiskBytes) {
    }
}

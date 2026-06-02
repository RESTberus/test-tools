package io.resttestgen.core.helper;

import io.resttestgen.core.datatype.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DeepReinforcementLearningProxy {

    private static final Logger logger = LogManager.getLogger(DeepReinforcementLearningProxy.class);

    private static String P2J_PIPE = "p2j";
    private static String J2P_PIPE = "j2p";
    private static FileInputStream p2jStream = null;
    private static FileOutputStream j2pStream = null;

    public static void initializeDeepReinforcementLearning(@NotNull List<String> namedPipesPaths, @NotNull Integer numOperations) {
        for (String namedPipesPath : namedPipesPaths) {
            Path p2jPipePath = Paths.get(namedPipesPath, "p2j");
            Path j2pPipePath = Paths.get(namedPipesPath, "j2p");
            if (Files.exists(p2jPipePath) && Files.exists(j2pPipePath)) {
                P2J_PIPE = p2jPipePath.toString();
                J2P_PIPE = j2pPipePath.toString();
                break;
            }
        }
        try {
            j2pStream = new FileOutputStream(J2P_PIPE);
            j2pStream.write((numOperations.toString() + "\n").getBytes());
            j2pStream.flush();
            p2jStream = new FileInputStream(P2J_PIPE);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @NotNull
    public static Integer getAction() {
        try {
            byte[] content = new byte[4];
            int bytesRead = p2jStream.read(content, 0, 4);
            if (bytesRead < 4) {
                logger.warn("Read less than 4 bytes from p2jStream");
            }
            String nextActionString = new String(content).trim();
            return Integer.parseInt(nextActionString);
        } catch (IOException | NumberFormatException e) {
            logger.error(e);
            return 0;
        }
    }

    public static void sendResult(@NotNull HttpStatusCode statusCode) {
        try {
            j2pStream.write((statusCode.toString() + "\n").getBytes());
            j2pStream.flush();
        } catch (IOException e) {
            logger.error(e);
        }
    }
}


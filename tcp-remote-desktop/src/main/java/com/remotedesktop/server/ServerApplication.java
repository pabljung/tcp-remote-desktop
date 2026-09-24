package com.remotedesktop.server;

import com.remotedesktop.protocol.MessageType;
import com.remotedesktop.server.ScreenCapture.ScreenTile;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servidor que transmite a tela em pequenos blocos e recebe comandos do mouse.
 */
public final class ServerApplication {

    private static final int PORT = 5000;
    private static final int TILE_SIZE = 128;
    private static final int FRAME_INTERVAL_MILLIS = 250;

    private ServerApplication() {
    }

    public static void main(String[] args) {
        System.out.println("Remote Desktop Server");

        try {
            ScreenCapture screenCapture = new ScreenCapture();
            RemoteInputController inputController = new RemoteInputController();
            startServer(screenCapture, inputController);
        } catch (AWTException exception) {
            System.err.println("Could not initialize screen access: " + exception.getMessage());
        }
    }

    private static void startServer(
            ScreenCapture screenCapture,
            RemoteInputController inputController) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Waiting for a client on port " + PORT + "...");

            try (Socket clientSocket = serverSocket.accept();
                 DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {

                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                inputController.setEnabled(true);
                runSession(input, output, screenCapture, inputController);
            } finally {
                inputController.setEnabled(false);
            }
        } catch (IOException exception) {
            System.err.println("Server error: " + exception.getMessage());
        }
    }

    private static void runSession(
            DataInputStream input,
            DataOutputStream output,
            ScreenCapture screenCapture,
            RemoteInputController inputController) throws IOException {
        AtomicBoolean connected = new AtomicBoolean(true);
        Thread commandThread = new Thread(
                () -> receiveCommands(input, inputController, connected),
                "remote-input-receiver");
        commandThread.setDaemon(true);
        commandThread.start();

        sendHandshake(output, screenCapture.getScreenWidth(), screenCapture.getScreenHeight());

        try {
            streamChangedTiles(output, screenCapture, connected);
        } finally {
            connected.set(false);
            commandThread.interrupt();
        }
    }

    private static void sendHandshake(DataOutputStream output, int screenWidth, int screenHeight)
            throws IOException {
        output.writeUTF(MessageType.HANDSHAKE.name());
        output.writeInt(screenWidth);
        output.writeInt(screenHeight);
        output.flush();
    }

    private static void streamChangedTiles(
            DataOutputStream output,
            ScreenCapture screenCapture,
            AtomicBoolean connected) throws IOException {
        Map<TilePosition, int[]> previousPixels = new HashMap<>();

        while (connected.get() && !Thread.currentThread().isInterrupted()) {
            List<ScreenTile> tiles = screenCapture.captureTiles(TILE_SIZE);

            for (ScreenTile tile : tiles) {
                TilePosition position = new TilePosition(tile.getX(), tile.getY());
                int[] currentPixels = readPixels(tile.getImage());
                int[] oldPixels = previousPixels.get(position);

                if (!Arrays.equals(currentPixels, oldPixels)) {
                    sendTile(output, tile);
                    previousPixels.put(position, currentPixels);
                }
            }

            output.flush();
            pauseBeforeNextFrame();
        }
    }

    private static int[] readPixels(BufferedImage image) {
        return image.getRGB(
                0,
                0,
                image.getWidth(),
                image.getHeight(),
                null,
                0,
                image.getWidth());
    }

    private static void sendTile(DataOutputStream output, ScreenTile tile) throws IOException {
        byte[] imageData = encodeTile(tile.getImage());

        output.writeUTF(MessageType.SCREEN_TILE.name());
        output.writeInt(tile.getX());
        output.writeInt(tile.getY());
        output.writeInt(tile.getWidth());
        output.writeInt(tile.getHeight());
        output.writeInt(imageData.length);
        output.write(imageData);
    }

    private static byte[] encodeTile(BufferedImage tile) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (!ImageIO.write(tile, "png", buffer)) {
            throw new IOException("PNG encoder is not available");
        }
        return buffer.toByteArray();
    }

    private static void pauseBeforeNextFrame() {
        try {
            Thread.sleep(FRAME_INTERVAL_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void receiveCommands(
            DataInputStream input,
            RemoteInputController inputController,
            AtomicBoolean connected) {
        try {
            while (connected.get()) {
                MessageType messageType = readMessageType(input);

                switch (messageType) {
                    case MOUSE_MOVE -> inputController.moveMouse(input.readInt(), input.readInt());
                    case MOUSE_CLICK -> inputController.clickMouse(
                            input.readInt(), input.readInt(), input.readInt());
                    case DISCONNECT -> connected.set(false);
                    case HANDSHAKE, SCREEN_TILE ->
                            throw new IOException("Unexpected client message: " + messageType);
                }
            }
        } catch (EOFException exception) {
            connected.set(false);
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            if (connected.getAndSet(false)) {
                System.err.println("Input connection error: " + exception.getMessage());
            }
        }
    }

    private static MessageType readMessageType(DataInputStream input) throws IOException {
        String typeName = input.readUTF();

        try {
            return MessageType.valueOf(typeName);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Unknown message type: " + typeName, exception);
        }
    }

    private record TilePosition(int x, int y) {
    }
}

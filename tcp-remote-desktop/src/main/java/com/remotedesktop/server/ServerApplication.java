package com.remotedesktop.server;

import com.remotedesktop.protocol.MessageType;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class ServerApplication {

    private static final int PORT = 5000;
    private static final int TILE_SIZE = 128;
    private static final int FRAME_INTERVAL_MILLIS = 250;

    private ServerApplication() {
    }

    private static void sendHandshake(DataOutputStream output, int screenWidth, int screenHeight)
            throws IOException {
        output.writeUTF(MessageType.HANDSHAKE.name());
        output.writeInt(screenWidth);
        output.writeInt(screenHeight);
        output.flush();
    }

    private static void streamScreen(DataOutputStream output, Robot robot, Rectangle screenBounds)
            throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            BufferedImage screen = robot.createScreenCapture(screenBounds);
            sendTiles(output, screen);
            output.flush();

            try {
                Thread.sleep(FRAME_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void sendTiles(DataOutputStream output, BufferedImage screen)
            throws IOException {
        for (int y = 0; y < screen.getHeight(); y += TILE_SIZE) {
            for (int x = 0; x < screen.getWidth(); x += TILE_SIZE) {
                int tileWidth = Math.min(TILE_SIZE, screen.getWidth() - x);
                int tileHeight = Math.min(TILE_SIZE, screen.getHeight() - y);
                BufferedImage tile = screen.getSubimage(x, y, tileWidth, tileHeight);
                byte[] imageData = encodeTile(tile);

                output.writeUTF(MessageType.SCREEN_TILE.name());
                output.writeInt(x);
                output.writeInt(y);
                output.writeInt(tileWidth);
                output.writeInt(tileHeight);
                output.writeInt(imageData.length);
                output.write(imageData);
            }
        }
    }

    private static byte[] encodeTile(BufferedImage tile) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (!ImageIO.write(tile, "png", buffer)) {
            throw new IOException("PNG encoder is not available");
        }
        return buffer.toByteArray();
    }

    public static void main(String[] args) {
        System.out.println("Remote Desktop Server");

        try {
            Robot robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenBounds = new Rectangle(screenSize);

            startServer(robot, screenBounds);
        } catch (AWTException exception) {
            System.err.println("Could not create the screen capture robot: " + exception.getMessage());
        }
    }

    private static void startServer(Robot robot, Rectangle screenBounds) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Waiting for a client on port " + PORT + "...");

            try (Socket clientSocket = serverSocket.accept();
                 DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {

                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                sendHandshake(output, screenBounds.width, screenBounds.height);
                streamScreen(output, robot, screenBounds);
            }
        } catch (IOException exception) {
            System.err.println("Server error: " + exception.getMessage());
        }
    }
}

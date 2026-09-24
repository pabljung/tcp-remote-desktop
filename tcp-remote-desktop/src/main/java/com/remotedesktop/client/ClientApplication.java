package com.remotedesktop.client;

import com.remotedesktop.protocol.MessageType;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

/**
 * Cliente responsável por receber e exibir pequenos blocos da tela remota.
 *
 * <p>O protocolo nunca envia a tela como um objeto único. Depois de um
 * {@link MessageType#HANDSHAKE}, cada {@link MessageType#SCREEN_TILE} contém a
 * posição, as dimensões e somente os bytes de imagem daquele bloco.</p>
 */
public final class ClientApplication {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 5000;
    private static final int MAX_TILE_BYTES = 4 * 1024 * 1024;

    private ClientApplication() {
        // Impede a instanciação da classe de inicialização.
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? parsePort(args[1]) : DEFAULT_PORT;

        System.out.println("Remote Desktop Client");

        try (Socket clientSocket = new Socket(host, port);
             DataInputStream input = new DataInputStream(clientSocket.getInputStream())) {

            System.out.println("Connected to " + host + ":" + port);
            ClientWindow window = createWindow();

            try {
                receiveMessages(input, window.panel());
            } finally {
                SwingUtilities.invokeLater(window.frame()::dispose);
            }
        } catch (IOException exception) {
            System.err.println("Client error: " + exception.getMessage());
        }
    }

    private static void receiveMessages(DataInputStream input, RemoteScreenPanel panel)
            throws IOException {
        boolean connected = true;

        while (connected) {
            MessageType messageType;

            try {
                messageType = readMessageType(input);
            } catch (EOFException exception) {
                System.out.println("Server closed the connection.");
                return;
            }

            connected = switch (messageType) {
                case HANDSHAKE -> {
                    receiveHandshake(input, panel);
                    yield true;
                }
                case SCREEN_TILE -> {
                    receiveScreenTile(input, panel);
                    yield true;
                }
                case DISCONNECT -> false;
                case MOUSE_MOVE, MOUSE_CLICK ->
                        throw new IOException("Unexpected server message: " + messageType);
            };
        }

        System.out.println("Server requested disconnection.");
    }

    private static MessageType readMessageType(DataInputStream input) throws IOException {
        String typeName = input.readUTF();

        try {
            return MessageType.valueOf(typeName);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Unknown message type: " + typeName, exception);
        }
    }

    private static void receiveHandshake(DataInputStream input, RemoteScreenPanel panel)
            throws IOException {
        int screenWidth = readPositiveInt(input, "screen width");
        int screenHeight = readPositiveInt(input, "screen height");

        panel.initializeScreen(screenWidth, screenHeight);
        System.out.println("Remote screen: " + screenWidth + "x" + screenHeight);
    }

    private static void receiveScreenTile(DataInputStream input, RemoteScreenPanel panel)
            throws IOException {
        int x = readNonNegativeInt(input, "tile x");
        int y = readNonNegativeInt(input, "tile y");
        int width = readPositiveInt(input, "tile width");
        int height = readPositiveInt(input, "tile height");
        int imageLength = readPositiveInt(input, "tile byte count");

        if (imageLength > MAX_TILE_BYTES) {
            throw new IOException("Tile exceeds the maximum size of " + MAX_TILE_BYTES + " bytes");
        }

        byte[] imageData = new byte[imageLength];
        input.readFully(imageData);

        BufferedImage tile = ImageIO.read(new ByteArrayInputStream(imageData));
        if (tile == null) {
            throw new IOException("Tile does not contain a supported image format");
        }
        if (tile.getWidth() != width || tile.getHeight() != height) {
            throw new IOException("Tile metadata does not match the decoded image dimensions");
        }

        try {
            panel.updateTile(x, y, tile);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IOException("Invalid tile: " + exception.getMessage(), exception);
        }
    }

    private static int readPositiveInt(DataInputStream input, String fieldName)
            throws IOException {
        int value = input.readInt();
        if (value <= 0) {
            throw new IOException(fieldName + " must be greater than zero");
        }
        return value;
    }

    private static int readNonNegativeInt(DataInputStream input, String fieldName)
            throws IOException {
        int value = input.readInt();
        if (value < 0) {
            throw new IOException(fieldName + " cannot be negative");
        }
        return value;
    }

    private static ClientWindow createWindow() throws IOException {
        ClientWindow[] result = new ClientWindow[1];

        try {
            SwingUtilities.invokeAndWait(() -> {
                RemoteScreenPanel panel = new RemoteScreenPanel();
                JFrame frame = new JFrame("TCP Remote Desktop");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(panel);
                frame.setSize(1024, 768);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                result[0] = new ClientWindow(frame, panel);
            });
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while creating the client window", exception);
        } catch (InvocationTargetException exception) {
            throw new IOException("Could not create the client window", exception.getCause());
        }

        return result[0];
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65_535) {
                throw new NumberFormatException();
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid port: " + value, exception);
        }
    }

    private record ClientWindow(JFrame frame, RemoteScreenPanel panel) {
    }
}

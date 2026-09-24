package com.remotedesktop.client;

import com.remotedesktop.protocol.MessageType;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cliente que reconstrói a tela recebida em blocos e envia comandos do mouse.
 */
public final class ClientApplication {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 5000;
    private static final int MAX_TILE_BYTES = 4 * 1024 * 1024;

    private ClientApplication() {
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? parsePort(args[1]) : DEFAULT_PORT;

        System.out.println("Remote Desktop Client");

        try (Socket socket = new Socket(host, port);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            System.out.println("Connected to " + host + ":" + port);
            ClientConnection connection = new ClientConnection(socket, output);
            ClientWindow window = createWindow(connection);

            try {
                receiveMessages(input, window.panel());
            } finally {
                connection.closeWithoutMessage();
                SwingUtilities.invokeLater(window.frame()::dispose);
            }
        } catch (IOException exception) {
            System.err.println("Client error: " + exception.getMessage());
        }
    }

    private static void receiveMessages(DataInputStream input, RemoteScreenPanel panel)
            throws IOException {
        while (true) {
            MessageType messageType;

            try {
                messageType = readMessageType(input);
            } catch (EOFException exception) {
                System.out.println("Server closed the connection.");
                return;
            }

            switch (messageType) {
                case HANDSHAKE -> receiveHandshake(input, panel);
                case SCREEN_TILE -> receiveScreenTile(input, panel);
                case DISCONNECT -> {
                    System.out.println("Server requested disconnection.");
                    return;
                }
                case MOUSE_MOVE, MOUSE_CLICK ->
                        throw new IOException("Unexpected server message: " + messageType);
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

    private static ClientWindow createWindow(ClientConnection connection) throws IOException {
        ClientWindow[] result = new ClientWindow[1];

        try {
            SwingUtilities.invokeAndWait(() -> {
                RemoteScreenPanel panel = new RemoteScreenPanel();
                installMouseControl(panel, connection);

                JFrame frame = new JFrame("TCP Remote Desktop");
                frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                frame.setContentPane(panel);
                frame.setSize(1024, 768);
                frame.setLocationRelativeTo(null);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent event) {
                        connection.disconnect();
                        frame.dispose();
                    }
                });
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

    private static void installMouseControl(
            RemoteScreenPanel panel,
            ClientConnection connection) {
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                sendMousePosition(panel, connection, event);
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                sendMousePosition(panel, connection, event);
            }
        });

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                Point remotePoint = panel.toRemotePoint(event.getX(), event.getY());
                if (remotePoint != null) {
                    connection.sendMouseClick(remotePoint.x, remotePoint.y, event.getButton());
                }
            }
        });
    }

    private static void sendMousePosition(
            RemoteScreenPanel panel,
            ClientConnection connection,
            MouseEvent event) {
        Point remotePoint = panel.toRemotePoint(event.getX(), event.getY());
        if (remotePoint != null) {
            connection.sendMouseMove(remotePoint.x, remotePoint.y);
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

    private static final class ClientConnection {

        private final Socket socket;
        private final DataOutputStream output;
        private final AtomicBoolean open = new AtomicBoolean(true);

        private ClientConnection(Socket socket, DataOutputStream output) {
            this.socket = socket;
            this.output = output;
        }

        private synchronized void sendMouseMove(int x, int y) {
            if (!open.get()) {
                return;
            }

            try {
                output.writeUTF(MessageType.MOUSE_MOVE.name());
                output.writeInt(x);
                output.writeInt(y);
                output.flush();
            } catch (IOException exception) {
                fail(exception);
            }
        }

        private synchronized void sendMouseClick(int x, int y, int button) {
            if (!open.get()) {
                return;
            }

            try {
                output.writeUTF(MessageType.MOUSE_CLICK.name());
                output.writeInt(x);
                output.writeInt(y);
                output.writeInt(button);
                output.flush();
            } catch (IOException exception) {
                fail(exception);
            }
        }

        private synchronized void disconnect() {
            if (!open.getAndSet(false)) {
                return;
            }

            try {
                output.writeUTF(MessageType.DISCONNECT.name());
                output.flush();
            } catch (IOException exception) {
                System.err.println("Could not send disconnect message: " + exception.getMessage());
            } finally {
                closeSocket();
            }
        }

        private synchronized void closeWithoutMessage() {
            if (open.getAndSet(false)) {
                closeSocket();
            }
        }

        private void fail(IOException exception) {
            System.err.println("Could not send mouse command: " + exception.getMessage());
            open.set(false);
            closeSocket();
        }

        private void closeSocket() {
            try {
                socket.close();
            } catch (IOException exception) {
                System.err.println("Could not close client socket: " + exception.getMessage());
            }
        }
    }
}

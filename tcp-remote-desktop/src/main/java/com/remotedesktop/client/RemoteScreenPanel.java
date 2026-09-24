package com.remotedesktop.client;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Reconstrói a tela remota desenhando cada bloco recebido em sua posição.
 */
public final class RemoteScreenPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private BufferedImage remoteScreen;

    public synchronized void initializeScreen(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Screen dimensions must be positive");
        }

        remoteScreen = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        SwingUtilities.invokeLater(() -> {
            setPreferredSize(new Dimension(width, height));
            revalidate();
            repaint();
        });
    }

    public synchronized void updateTile(int x, int y, BufferedImage tile) {
        if (remoteScreen == null) {
            throw new IllegalStateException("A handshake must initialize the screen first");
        }
        if (x < 0 || y < 0
                || x + tile.getWidth() > remoteScreen.getWidth()
                || y + tile.getHeight() > remoteScreen.getHeight()) {
            throw new IllegalArgumentException("Tile is outside the remote screen bounds");
        }

        Graphics graphics = remoteScreen.getGraphics();
        try {
            graphics.drawImage(tile, x, y, null);
        } finally {
            graphics.dispose();
        }

        repaint(x, y, tile.getWidth(), tile.getHeight());
    }

    @Override
    protected synchronized void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (remoteScreen != null) {
            graphics.drawImage(remoteScreen, 0, 0, this);
        }
    }
}

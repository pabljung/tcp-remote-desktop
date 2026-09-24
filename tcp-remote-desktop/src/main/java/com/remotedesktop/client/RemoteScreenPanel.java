package com.remotedesktop.client;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Point;
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
        repaint();
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

        repaint();
    }

    public synchronized Point toRemotePoint(int panelX, int panelY) {
        if (remoteScreen == null || getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }

        int remoteX = panelX * remoteScreen.getWidth() / getWidth();
        int remoteY = panelY * remoteScreen.getHeight() / getHeight();
        remoteX = Math.max(0, Math.min(remoteX, remoteScreen.getWidth() - 1));
        remoteY = Math.max(0, Math.min(remoteY, remoteScreen.getHeight() - 1));
        return new Point(remoteX, remoteY);
    }

    @Override
    protected synchronized void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (remoteScreen != null) {
            graphics.drawImage(remoteScreen, 0, 0, getWidth(), getHeight(), this);
        }
    }
}

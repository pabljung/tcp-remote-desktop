package com.remotedesktop.server;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Captura a tela principal e a divide em blocos para processamento posterior.
 */
public final class ScreenCapture {

    private final Robot robot;
    private final Rectangle screenBounds;

    public ScreenCapture() throws AWTException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new AWTException("A captura exige um ambiente grafico.");
        }

        GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        this.screenBounds = screen.getDefaultConfiguration().getBounds();
        this.robot = new Robot(screen);
    }

    public BufferedImage captureScreen() {
        return robot.createScreenCapture(screenBounds);
    }

    public int getScreenWidth() {
        return screenBounds.width;
    }

    public int getScreenHeight() {
        return screenBounds.height;
    }

    public List<ScreenTile> splitIntoTiles(BufferedImage image, int tileSize) {
        if (image == null) {
            throw new IllegalArgumentException("A imagem nao pode ser nula.");
        }
        if (tileSize <= 0) {
            throw new IllegalArgumentException("O tamanho do bloco deve ser positivo.");
        }

        List<ScreenTile> tiles = new ArrayList<>();
        for (int y = 0; y < image.getHeight();) {
            for (int x = 0; x < image.getWidth();) {
                int width = Math.min(tileSize, image.getWidth() - x);
                int height = Math.min(tileSize, image.getHeight() - y);
                BufferedImage tileImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = tileImage.createGraphics();
                try {
                    graphics.drawImage(image, 0, 0, width, height,
                            x, y, x + width, y + height, null);
                } finally {
                    graphics.dispose();
                }
                tiles.add(new ScreenTile(x, y, tileImage));
                x += width;
            }
            y += Math.min(tileSize, image.getHeight() - y);
        }
        return tiles;
    }

    public List<ScreenTile> captureTiles(int tileSize) {
        return splitIntoTiles(captureScreen(), tileSize);
    }

    public static final class ScreenTile {
        private final int x;
        private final int y;
        private final BufferedImage image;

        public ScreenTile(int x, int y, BufferedImage image) {
            this.x = x;
            this.y = y;
            this.image = Objects.requireNonNull(image, "image");
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public BufferedImage getImage() {
            return image;
        }

        public int getWidth() {
            return image.getWidth();
        }

        public int getHeight() {
            return image.getHeight();
        }
    }
}

package com.remotedesktop.server;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * Aplica movimentos e cliques do mouse na tela principal do servidor.
 * Os comandos são executados somente quando o controle está habilitado.
 */
public final class RemoteInputController {

    // Parte 1: atributos
    private final Robot robot;
    private final Rectangle screenBounds;
    private boolean enabled;

    // Parte 2: preparação do Robot
    public RemoteInputController() throws AWTException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new AWTException("O controle do mouse exige uma interface gráfica.");
        }

        GraphicsDevice screen = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        this.screenBounds = screen.getDefaultConfiguration().getBounds();
        this.robot = new Robot(screen);
        this.enabled = false;
    }

    // Parte 3: ligar ou desligar o controle
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Parte 4: movimentar o mouse
    public void moveMouse(int x, int y) {
        checkEnabled();
        checkCoordinates(x, y);
        robot.mouseMove(x, y);
    }

    // Parte 5: clicar com o mouse
    public void clickMouse(int x, int y, int button) {
        checkEnabled();
        checkCoordinates(x, y);

        int buttonMask = getButtonMask(button);

        robot.mouseMove(x, y);
        robot.mousePress(buttonMask);

        try {
            // O botão fica pressionado apenas durante este bloco.
        } finally {
            robot.mouseRelease(buttonMask);
        }
    }

    // Parte 6: validações
    private void checkEnabled() {
        if (!enabled) {
            throw new IllegalStateException("O controle remoto está desabilitado.");
        }
    }

    private void checkCoordinates(int x, int y) {
        if (!screenBounds.contains(x, y)) {
            throw new IllegalArgumentException(
                    "Coordenadas fora da tela: x=" + x + ", y=" + y);
        }
    }

    private int getButtonMask(int button) {
        switch (button) {
            case MouseEvent.BUTTON1:
                return InputEvent.BUTTON1_DOWN_MASK;
            case MouseEvent.BUTTON2:
                return InputEvent.BUTTON2_DOWN_MASK;
            case MouseEvent.BUTTON3:
                return InputEvent.BUTTON3_DOWN_MASK;
            default:
                throw new IllegalArgumentException(
                        "Botão inválido. Use 1 (esquerdo), 2 (meio) ou 3 (direito).");
        }
    }
}
package com.remotedesktop.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Ponto de entrada do futuro servidor de acesso remoto.
 */
public final class ServerApplication {

    private static final int PORT = 5000;

    private ServerApplication() {
        // Impede a instanciação da classe de inicialização.
    }

    public static void main(String[] args) {
        System.out.println("Remote Desktop Server");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Waiting for a client on port " + PORT + "...");

            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                // TODO: Trocar mensagens simples com o cliente na próxima etapa.
            }
        } catch (IOException exception) {
            System.err.println("Server error: " + exception.getMessage());
        }
    }
}

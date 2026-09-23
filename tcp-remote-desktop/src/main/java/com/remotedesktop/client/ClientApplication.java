package com.remotedesktop.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Ponto de entrada do futuro cliente de acesso remoto.
 */
public final class ClientApplication {

    private ClientApplication() {
        // Impede a instanciação da classe de inicialização.
    }

    public static void main(String[] args) throws UnknownHostException, IOException {
        System.out.println("Remote Desktop Client");
        initClient();
    }

    private static void initClient() throws UnknownHostException, IOException {
        try (
                Socket cliente = new Socket("127.0.0.1", 5000);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(cliente.getInputStream()))) {
            System.out.println("Conectado ao servidor");

            String mensagem;

            while ((mensagem = reader.readLine()) != null) {
                System.out.println("Servidor: " + mensagem);
            }

            System.out.println("Conexão encerrada.");
        }
    }
}

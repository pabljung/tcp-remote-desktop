package com.remotedesktop.protocol;

import java.util.Objects;

/**
 * Representação conceitual mínima de uma mensagem trocada entre cliente e
 * servidor. A serialização e os campos específicos de cada tipo serão
 * definidos futuramente.
 */
public final class ProtocolMessage {

    private final MessageType type;

    public ProtocolMessage(MessageType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public MessageType getType() {
        return type;
    }
}

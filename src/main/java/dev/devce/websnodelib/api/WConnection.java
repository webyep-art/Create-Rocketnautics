package dev.devce.websnodelib.api;

import java.util.UUID;

public record WConnection(
    UUID sourceNode,
    int sourcePin,
    UUID targetNode,
    int targetPin
) {}

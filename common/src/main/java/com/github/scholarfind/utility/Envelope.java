package com.github.scholarfind.utility;

import com.github.scholarfind.api.queue.Acknowledgement;

public record Envelope<T>(T document, Acknowledgement acknowledgement) {
}

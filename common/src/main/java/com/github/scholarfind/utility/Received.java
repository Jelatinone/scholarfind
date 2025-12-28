package com.github.scholarfind.utility;

public record Received<T>(T document, String receiptHandle) {
}

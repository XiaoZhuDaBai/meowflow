package com.meowflow.plugin.sdk;

public class PluginClientException extends RuntimeException {

    public PluginClientException(String message) {
        super(message);
    }

    public PluginClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

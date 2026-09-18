#!/bin/bash
# RabbitMQ initialization script
# Enable required plugins: management console + delayed-message-exchange

set -e

PLUGIN_DIR=/opt/rabbitmq/plugins
PLUGIN_VERSION=3.12.0
PLUGIN_NAME=rabbitmq_delayed_message_exchange-${PLUGIN_VERSION}

echo "Checking RabbitMQ plugins..."

# Enable management console (built-in)
rabbitmq-plugins enable --offline rabbitmq_management

# Enable delayed-message-exchange plugin
# First check if already enabled
if rabbitmq-plugins list -e | grep -q rabbitmq_delayed_message_exchange; then
    echo "Plugin rabbitmq_delayed_message_exchange already enabled"
else
    echo "Attempting to enable rabbitmq_delayed_message_exchange..."

    # Try to enable (works if plugin file exists)
    if rabbitmq-plugins enable --offline rabbitmq_delayed_message_exchange 2>/dev/null; then
        echo "Plugin rabbitmq_delayed_message_exchange enabled successfully"
    else
        # If plugin file doesn't exist, try to download it
        echo "Plugin file not found, attempting to download..."

        # Check if we can reach the internet
        if curl -sf https://github.com > /dev/null 2>&1; then
            curl -fL "https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v${PLUGIN_VERSION}/${PLUGIN_NAME}.ez" \
                -o "${PLUGIN_DIR}/${PLUGIN_NAME}.ez" || {
                    echo "Failed to download plugin from GitHub"
                    echo "Warning: Delayed message exchange will not be available"
                }

            if [ -f "${PLUGIN_DIR}/${PLUGIN_NAME}.ez" ]; then
                rabbitmq-plugins enable --offline rabbitmq_delayed_message_exchange
                echo "Plugin downloaded and enabled successfully"
            fi
        else
            echo "Warning: Cannot reach internet to download plugin"
            echo "Warning: Delayed message exchange will not be available"
            echo "To fix: Download rabbitmq_delayed_message_exchange-${PLUGIN_VERSION}.ez manually"
            echo "  from: https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases"
            echo "  to:   ./deploy/docker/rabbitmq_delayed_message_exchange-${PLUGIN_VERSION}.ez"
        fi
    fi
fi

echo ""
echo "Enabled plugins:"
rabbitmq-plugins list -e

# Extend official RabbitMQ image with delayed-message-exchange plugin
FROM rabbitmq:3.12-management-alpine

# Copy pre-downloaded plugin (bundled with docker-compose)
COPY rabbitmq_delayed_message_exchange-3.12.0.ez /opt/rabbitmq/plugins/

# Install plugin and enable required plugins
RUN rabbitmq-plugins enable --offline rabbitmq_management rabbitmq_delayed_message_exchange

# Ensure plugins are enabled on every startup
RUN echo '[rabbitmq_management,rabbitmq_delayed_message_exchange].' > /etc/rabbitmq/enabled_plugins

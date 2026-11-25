-- Die Outbox wird nicht mehr verwendet, Nachrichten gehen direkt nach dem Commit an RabbitMQ
drop table if exists outbox_messages;

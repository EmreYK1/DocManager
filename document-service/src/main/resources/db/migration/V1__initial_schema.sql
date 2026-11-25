-- Stand, den Hibernate früher mit ddl-auto=update angelegt hat.
-- Auf solchen bestehenden Datenbanken wird dieses Skript übersprungen (Flyway-Baseline).
create table folders (
    id        uuid primary key,
    name      varchar(255) not null,
    parent_id uuid references folders (id)
);

create table documents (
    id           uuid primary key,
    filename     varchar(255) not null,
    content_type varchar(255) not null,
    size_bytes   bigint not null,
    uploaded_at  timestamp(6) with time zone not null,
    status       varchar(255) not null
        check (status in ('UPLOADED', 'OCR_PENDING', 'OCR_DONE', 'SUMMARY_DONE', 'FAILED')),
    ocr_text     text,
    summary      text,
    folder_id    uuid references folders (id),
    object_key   varchar(255)
);

create table comments (
    id          uuid primary key,
    content     varchar(2000) not null,
    author      varchar(255) not null,
    created_at  timestamp(6) with time zone not null,
    document_id uuid not null references documents (id)
);

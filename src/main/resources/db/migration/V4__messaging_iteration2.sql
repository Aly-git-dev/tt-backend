-- ============================================================================
-- V3__messaging_iteration2.sql
-- Módulo: Mensajería 1:1 (Profesor-Alumno, Profesor-Asesor, Alumno-Asesor)
-- + Adjuntos (filesystem) + Reportes con contexto (msg + 5 prev) + Sanciones Admin
-- Requiere: V1__init_final.sql (users, roles, user_roles, trg_set_timestamp, pgcrypto)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Tabla: chat_conversation (DIRECT 1:1)
--    Usamos user1_id y user2_id para asegurar 1:1
-- ----------------------------------------------------------------------------

CREATE TABLE chat_conversation (
                                   id            BIGSERIAL PRIMARY KEY,

                                   user1_id       UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                   user2_id       UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,

    -- etiqueta para auditoría (la validación fuerte se hace en backend)
                                   allowed_pair   VARCHAR(30) NOT NULL,  -- 'PROFESOR-ALUMNO' | 'PROFESOR-ASESOR' | 'ALUMNO-ASESOR'

    -- útil para listar conversaciones estilo "inbox"
                                   last_message_at TIMESTAMP WITHOUT TIME ZONE,

                                   created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                   updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                                   CONSTRAINT ck_chat_conversation_distinct_users CHECK (user1_id <> user2_id),
                                   CONSTRAINT ck_chat_allowed_pair CHECK (
                                       allowed_pair IN ('PROFESOR-ALUMNO', 'PROFESOR-ASESOR', 'ALUMNO-ASESOR')
                                       )
);

-- Unicidad de conversación por pareja (sin importar el orden)
CREATE UNIQUE INDEX ux_chat_conversation_pair
    ON chat_conversation (LEAST(user1_id, user2_id), GREATEST(user1_id, user2_id));

CREATE INDEX idx_chat_conversation_user1     ON chat_conversation(user1_id);
CREATE INDEX idx_chat_conversation_user2     ON chat_conversation(user2_id);
CREATE INDEX idx_chat_conversation_pair      ON chat_conversation(allowed_pair);
CREATE INDEX idx_chat_conversation_lastmsg   ON chat_conversation(last_message_at DESC);

CREATE TRIGGER set_timestamp_chat_conversation
    BEFORE UPDATE ON chat_conversation
    FOR EACH ROW EXECUTE FUNCTION trg_set_timestamp();


-- ----------------------------------------------------------------------------
-- 2) Tabla: chat_message
-- ----------------------------------------------------------------------------

CREATE TABLE chat_message (
                              id               BIGSERIAL PRIMARY KEY,
                              conversation_id  BIGINT NOT NULL REFERENCES chat_conversation(id)
                                  ON DELETE CASCADE ON UPDATE CASCADE,

                              sender_id        UUID   NOT NULL REFERENCES users(id)
                                  ON DELETE RESTRICT ON UPDATE CASCADE,

                              content          TEXT,
                              content_type     VARCHAR(20) NOT NULL DEFAULT 'TEXT', -- TEXT | FILE | MIXED | SYSTEM

    -- idempotencia (si el front reintenta el POST por red)
                              client_message_id VARCHAR(80),

                              created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                              updated_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

    -- estados opcionales si luego quieres "read receipts"
                              status           VARCHAR(20) NOT NULL DEFAULT 'SENT',  -- SENT | DELIVERED | READ

    -- No permitimos texto en blanco (si viene texto). La regla de "vacío sin adjunto"
    -- se valida en backend (porque el adjunto está en otra tabla).
                              CONSTRAINT ck_chat_message_content_not_blank
                                  CHECK (content IS NULL OR length(trim(content)) > 0)
);

CREATE INDEX idx_chat_message_conversation_created
    ON chat_message(conversation_id, created_at DESC);

CREATE INDEX idx_chat_message_sender
    ON chat_message(sender_id);

CREATE INDEX idx_chat_message_type
    ON chat_message(content_type);

-- Único por conversación + sender + client_message_id (solo si viene)
CREATE UNIQUE INDEX ux_chat_message_idempotency
    ON chat_message(conversation_id, sender_id, client_message_id)
    WHERE client_message_id IS NOT NULL;

CREATE TRIGGER set_timestamp_chat_message
    BEFORE UPDATE ON chat_message
    FOR EACH ROW EXECUTE FUNCTION trg_set_timestamp();


-- ----------------------------------------------------------------------------
-- 3) Tabla: chat_attachment (adjuntos guardados en filesystem)
--    Guardamos ruta y metadatos (NO servicio externo)
-- ----------------------------------------------------------------------------

CREATE TABLE chat_attachment (
                                 id             BIGSERIAL PRIMARY KEY,
                                 message_id     BIGINT NOT NULL REFERENCES chat_message(id)
                                     ON DELETE CASCADE ON UPDATE CASCADE,

                                 original_name  VARCHAR(255) NOT NULL,
                                 mime_type      VARCHAR(120) NOT NULL,
                                 size_bytes     BIGINT       NOT NULL,
                                 storage_path   TEXT         NOT NULL,   -- ej: /data/chat-uploads/{convId}/{msgId}/{uuid}
                                 checksum       VARCHAR(128),            -- opcional (sha256)

                                 created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_attachment_message ON chat_attachment(message_id);
CREATE INDEX idx_chat_attachment_mime    ON chat_attachment(mime_type);


-- ----------------------------------------------------------------------------
-- 4) Tabla: chat_message_report
--    Reporte de un mensaje (motivo + descripción), gestionado por ADMIN
-- ----------------------------------------------------------------------------

CREATE TABLE chat_message_report (
                                     id                   BIGSERIAL PRIMARY KEY,

                                     reporter_id          UUID   NOT NULL REFERENCES users(id)
                                         ON DELETE RESTRICT ON UPDATE CASCADE,

                                     conversation_id      BIGINT NOT NULL REFERENCES chat_conversation(id)
                                         ON DELETE CASCADE ON UPDATE CASCADE,

                                     reported_message_id  BIGINT NOT NULL REFERENCES chat_message(id)
                                         ON DELETE CASCADE ON UPDATE CASCADE,

                                     reason_code          VARCHAR(30) NOT NULL, -- SPAM / OFENSIVO / AMENAZA / ACOSO / OTRO...
                                     description          TEXT,

                                     status               VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE | EN_REVISION | RESUELTO | DESESTIMADO
                                     handled_by           UUID REFERENCES users(id)
                                         ON DELETE SET NULL ON UPDATE CASCADE,

                                     handled_at           TIMESTAMP WITHOUT TIME ZONE,
                                     created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                                     CONSTRAINT ck_chat_report_status CHECK (
                                         status IN ('PENDIENTE','EN_REVISION','RESUELTO','DESESTIMADO')
                                         )
);

CREATE INDEX idx_chat_report_status        ON chat_message_report(status);
CREATE INDEX idx_chat_report_reporter      ON chat_message_report(reporter_id);
CREATE INDEX idx_chat_report_conversation  ON chat_message_report(conversation_id);
CREATE INDEX idx_chat_report_message       ON chat_message_report(reported_message_id);


-- ----------------------------------------------------------------------------
-- 5) Tabla: chat_report_context
--    Copia del mensaje reportado + 5 anteriores (snapshot para contexto)
--    context_index: 0 = mensaje reportado, 1..5 = anteriores
-- ----------------------------------------------------------------------------

CREATE TABLE chat_report_context (
                                     report_id              BIGINT NOT NULL REFERENCES chat_message_report(id)
                                         ON DELETE CASCADE ON UPDATE CASCADE,

                                     context_index          SMALLINT NOT NULL, -- 0..5
                                     message_id             BIGINT REFERENCES chat_message(id)
                                         ON DELETE SET NULL ON UPDATE CASCADE,

                                     sender_id_snapshot     UUID,
                                     sender_role_snapshot   VARCHAR(30), -- ADMIN/PROFESOR/ALUMNO/ASESOR (valor copiado al reportar)

                                     content_snapshot       TEXT,
                                     content_type_snapshot  VARCHAR(20),
                                     created_at_snapshot    TIMESTAMP WITHOUT TIME ZONE,

                                     PRIMARY KEY (report_id, context_index),

                                     CONSTRAINT ck_chat_report_context_index
                                         CHECK (context_index >= 0 AND context_index <= 5)
);

CREATE INDEX idx_chat_report_context_report ON chat_report_context(report_id);


-- ----------------------------------------------------------------------------
-- 6) Tabla: chat_sanction
--    Acciones de moderación de ADMIN sobre usuario, ligadas opcionalmente a un reporte
-- ----------------------------------------------------------------------------

CREATE TABLE chat_sanction (
                               id              BIGSERIAL PRIMARY KEY,

                               report_id        BIGINT REFERENCES chat_message_report(id)
                                   ON DELETE SET NULL ON UPDATE CASCADE,

                               target_user_id   UUID NOT NULL REFERENCES users(id)
                                   ON DELETE RESTRICT ON UPDATE CASCADE,

                               admin_id         UUID NOT NULL REFERENCES users(id)
                                   ON DELETE RESTRICT ON UPDATE CASCADE,

                               type             VARCHAR(20) NOT NULL, -- WARNING | TEMP_BLOCK | BAN | MUTE
                               notes            TEXT,

                               start_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                               end_at           TIMESTAMP WITHOUT TIME ZONE, -- null = indefinido (ej BAN)

                               created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

                               CONSTRAINT ck_chat_sanction_type CHECK (
                                   type IN ('WARNING','TEMP_BLOCK','BAN','MUTE')
                                   ),
                               CONSTRAINT ck_chat_sanction_dates CHECK (
                                   end_at IS NULL OR end_at >= start_at
                                   )
);

CREATE INDEX idx_chat_sanction_target ON chat_sanction(target_user_id);
CREATE INDEX idx_chat_sanction_admin  ON chat_sanction(admin_id);
CREATE INDEX idx_chat_sanction_type   ON chat_sanction(type);
CREATE INDEX idx_chat_sanction_report ON chat_sanction(report_id);

-- ============================================================================
-- FIN V3 – Mensajería 1:1 + Adjuntos + Reportes + Moderación
-- ============================================================================

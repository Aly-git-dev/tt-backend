-- EXTENSION SEGURA
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- TABLA SEGURA
CREATE TABLE IF NOT EXISTS teacher_evaluation (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL,
    evaluator_id UUID,
    appointment_id UUID,
    rating_clarity SMALLINT NOT NULL,
    rating_knowledge SMALLINT NOT NULL,
    rating_support SMALLINT NOT NULL,
    rating_punctuality SMALLINT NOT NULL,
    comment TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
    );

-- VISTA SEGURA
CREATE OR REPLACE VIEW vw_admin_topic_interest AS
SELECT
    fc.id,
    fc.name,
    COUNT(*) as total
FROM topic_interest_event tie
         JOIN forum_category fc ON fc.id = tie.category_id
GROUP BY fc.id, fc.name;
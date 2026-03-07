-- V__create_agenda_tables.sql

create table appointments (
                              id bigserial primary key,
                              title varchar(120) not null,
                              description text,
                              modality varchar(20) not null, -- ONLINE | PRESENCIAL
                              starts_at timestamp not null,
                              ends_at timestamp not null,
                              status varchar(20) not null,   -- SCHEDULED | CANCELLED | COMPLETED
                              created_by bigint not null,
                              created_at timestamp not null default now(),
                              updated_at timestamp not null default now()
);

create table appointment_participants (
                                          appointment_id bigint not null references appointments(id) on delete cascade,
                                          user_id bigint not null,
                                          role varchar(20) not null,  -- HOST | ATTENDEE
                                          rsvp varchar(20) not null,  -- PENDING | ACCEPTED | DECLINED | TENTATIVE
                                          primary key (appointment_id, user_id)
);

create table reminders (
                           id bigserial primary key,
                           target_type varchar(20) not null,  -- APPOINTMENT (por ahora)
                           target_id bigint not null,         -- appointment id
                           user_id bigint not null,
                           channel varchar(20) not null,      -- IN_APP | PUSH
                           remind_at timestamp not null,
                           sent_at timestamp null,
                           created_at timestamp not null default now()
);

create index idx_reminders_due on reminders (remind_at) where sent_at is null;
create index idx_participants_user on appointment_participants (user_id);

create table notifications (
                               id bigserial primary key,
                               user_id bigint not null,
                               type varchar(40) not null,         -- INVITE | RESCHEDULED | CANCELLED | REMINDER
                               title varchar(140) not null,
                               body text,
                               target_type varchar(20),
                               target_id bigint,
                               read_at timestamp null,
                               created_at timestamp not null default now()
);

create index idx_notifications_user on notifications (user_id, read_at);

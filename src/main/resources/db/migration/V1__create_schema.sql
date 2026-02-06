CREATE TABLE IF NOT EXISTS public.dopaminelite_issues
(
    id uuid NOT NULL,
    assigned_admin_id uuid,
    assignment_status character varying(255) COLLATE pg_catalog."default" NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
                                description character varying(255) COLLATE pg_catalog."default" NOT NULL,
    is_chat_read_only boolean NOT NULL,
    solved_at timestamp(6) with time zone,
                                status character varying(255) COLLATE pg_catalog."default" NOT NULL,
    student_id uuid NOT NULL,
    title character varying(255) COLLATE pg_catalog."default" NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
                                issue_number bigint NOT NULL,
                                CONSTRAINT dopaminelite_issues_pkey PRIMARY KEY (id),
    CONSTRAINT ukdme9fgspcnn3cabc1dx18fhk6 UNIQUE (issue_number),
    CONSTRAINT dopaminelite_issues_assignment_status_check CHECK (assignment_status::text = ANY (ARRAY['UNASSIGNED'::character varying, 'ASSIGNED'::character varying]::text[])),
    CONSTRAINT dopaminelite_issues_status_check CHECK (status::text = ANY (ARRAY['OPEN'::character varying, 'IN_PROGRESS'::character varying, 'SOLVED'::character varying]::text[]))
);

CREATE TABLE IF NOT EXISTS public.dopaminelite_stored_files
(
    id uuid NOT NULL,
    bucket character varying(255) COLLATE pg_catalog."default" NOT NULL,
    context_ref_id character varying(255) COLLATE pg_catalog."default",
    context_type character varying(255) COLLATE pg_catalog."default" NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by_user_id uuid NOT NULL,
    is_deleted boolean NOT NULL,
    mime_type character varying(255) COLLATE pg_catalog."default" NOT NULL,
    original_file_name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    sha256 character varying(255) COLLATE pg_catalog."default",
    size_bytes bigint NOT NULL,
    storage_path character varying(255) COLLATE pg_catalog."default" NOT NULL,
    stored_file_name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT dopaminelite_stored_files_pkey PRIMARY KEY (id),
    CONSTRAINT ukdr5rb1a4w07osewvh4rhkh19m UNIQUE (stored_file_name),
    CONSTRAINT ukpkuww0nihrbjf2vcv8xq88r37 UNIQUE (storage_path),
    CONSTRAINT dopaminelite_stored_files_context_type_check CHECK (context_type::text = ANY (ARRAY['PAYMENT_SUBMISSION'::character varying, 'ISSUE_ATTACHMENT'::character varying, 'DOCUMENT'::character varying, 'OTHER'::character varying]::text[]))
);

CREATE TABLE IF NOT EXISTS public.dopaminelite_issue_messages
(
    id uuid NOT NULL,
    file_id character varying(255) COLLATE pg_catalog."default",
    file_name character varying(255) COLLATE pg_catalog."default",
    file_type character varying(255) COLLATE pg_catalog."default",
    content text COLLATE pg_catalog."default" NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
                                issue_id uuid NOT NULL,
                                sender_id uuid NOT NULL,
                                sender_role character varying(255) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT dopaminelite_issue_messages_pkey PRIMARY KEY (id),
    CONSTRAINT dopaminelite_issue_messages_sender_role_check CHECK (sender_role::text = ANY (ARRAY['STUDENT'::character varying, 'ADMIN'::character varying, 'MAIN_ADMIN'::character varying]::text[]))
);

CREATE TABLE IF NOT EXISTS public.dopaminelite_issue_attachments
(
    issue_id uuid NOT NULL,
    file_id character varying(255) COLLATE pg_catalog."default",
    file_name character varying(255) COLLATE pg_catalog."default",
    file_type character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT fklk85y68ay5gr1biwb0jxco3b3 FOREIGN KEY (issue_id)
    REFERENCES public.dopaminelite_issues (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
);


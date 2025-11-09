-- ==========================================================
-- 🧱 INIT-MSTCC.SQL
-- Inicialização automática dos bancos PostgreSQL
-- para os microsserviços do projeto MSTCC
-- ==========================================================

-- Criar bancos de dados (se não existirem)
DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mstcc_user') THEN
      PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE mstcc_user');
   END IF;

   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mstcc_post') THEN
      PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE mstcc_post');
   END IF;

   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mstcc_comment') THEN
      PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE mstcc_comment');
   END IF;

   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mstcc_like') THEN
      PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE mstcc_like');
   END IF;

   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mstcc_friendship') THEN
      PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE mstcc_friendship');
   END IF;
END $$ LANGUAGE plpgsql;

-- ==========================================================
-- Criar schema "public" em cada banco (caso necessário)
-- ==========================================================

\connect mstcc_user
CREATE SCHEMA IF NOT EXISTS public AUTHORIZATION postgres;

\connect mstcc_post
CREATE SCHEMA IF NOT EXISTS public AUTHORIZATION postgres;

\connect mstcc_comment
CREATE SCHEMA IF NOT EXISTS public AUTHORIZATION postgres;

\connect mstcc_like
CREATE SCHEMA IF NOT EXISTS public AUTHORIZATION postgres;

\connect mstcc_friendship
CREATE SCHEMA IF NOT EXISTS public AUTHORIZATION postgres;

-- ==========================================================
-- 🧠 Logs de sucesso
-- ==========================================================
\echo '✅ Todos os bancos e schemas foram criados com sucesso!'

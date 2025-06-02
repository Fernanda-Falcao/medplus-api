

-- Flyway Migration Script: V2__insert_initial_profiles.sql
-- Insere os perfis (roles) iniciais no sistema.

-- Verifica se os perfis já existem para evitar erros em re-execuções (embora Flyway gerencie isso)
-- No entanto, para idempotência manual, pode ser útil.
-- Flyway por padrão não re-executa scripts já aplicados.

-- Inserir Perfil PACIENTE
INSERT INTO tb_perfil (nome)
SELECT 'ROLE_PACIENTE'
WHERE NOT EXISTS (SELECT 1 FROM tb_perfil WHERE nome = 'ROLE_PACIENTE');

-- Inserir Perfil MEDICO
INSERT INTO tb_perfil (nome)
SELECT 'ROLE_MEDICO'
WHERE NOT EXISTS (SELECT 1 FROM tb_perfil WHERE nome = 'ROLE_MEDICO');

-- Inserir Perfil ADMIN
INSERT INTO tb_perfil (nome)
SELECT 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM tb_perfil WHERE nome = 'ROLE_ADMIN');


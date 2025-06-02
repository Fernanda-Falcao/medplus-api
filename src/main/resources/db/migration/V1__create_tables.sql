

-- Flyway Migration Script: V1__create_tables.sql
-- Este é um EXEMPLO MUITO BÁSICO. Você precisará ajustá-lo
-- com todos os campos, tipos corretos, constraints, índices, etc.,
-- para corresponder às suas entidades JPA.

-- Tabela de Perfis (Roles)
CREATE TABLE tb_perfil (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(20) NOT NULL UNIQUE
);

-- Tabela base de Usuários
CREATE TABLE tb_usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) NOT NULL UNIQUE,
    data_nascimento DATE,
    telefone VARCHAR(20),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    data_atualizacao TIMESTAMP WITHOUT TIME ZONE,
    -- Campos de Endereço (embutido)
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf VARCHAR(2),
    cep VARCHAR(9)
);

-- Tabela de junção Usuário-Perfil
CREATE TABLE tb_usuario_perfil (
    usuario_id BIGINT NOT NULL,
    perfil_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, perfil_id),
    FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id) ON DELETE CASCADE,
    FOREIGN KEY (perfil_id) REFERENCES tb_perfil (id)
);

-- Tabela de Pacientes (herda de Usuário)
CREATE TABLE tb_paciente (
    usuario_id BIGINT PRIMARY KEY, -- Mesma PK que tb_usuario
    historico_medico TEXT,
    FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id) ON DELETE CASCADE
);

-- Tabela de Médicos (herda de Usuário)
CREATE TABLE tb_medico (
    usuario_id BIGINT PRIMARY KEY, -- Mesma PK que tb_usuario
    crm VARCHAR(20) NOT NULL UNIQUE,
    especialidade VARCHAR(100) NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id) ON DELETE CASCADE
);

-- Tabela de Administradores (herda de Usuário)
CREATE TABLE tb_admin (
    usuario_id BIGINT PRIMARY KEY, -- Mesma PK que tb_usuario
    nivel_acesso INTEGER,
    FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id) ON DELETE CASCADE
);

-- Tabela de Disponibilidade do Médico
CREATE TABLE tb_disponibilidade_medico (
    id BIGSERIAL PRIMARY KEY,
    medico_id BIGINT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL, -- Ex: MONDAY, TUESDAY
    hora_inicio TIME WITHOUT TIME ZONE NOT NULL,
    hora_fim TIME WITHOUT TIME ZONE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    data_atualizacao TIMESTAMP WITHOUT TIME ZONE,
    FOREIGN KEY (medico_id) REFERENCES tb_medico (usuario_id) ON DELETE CASCADE,
    UNIQUE (medico_id, dia_semana, hora_inicio, hora_fim) -- Garante que não haja duplicatas exatas
);

-- Tabela de Consultas
CREATE TABLE tb_consulta (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    medico_id BIGINT NOT NULL,
    data_hora_consulta TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status_consulta VARCHAR(30) NOT NULL,
    observacoes TEXT,
    motivo_cancelamento TEXT,
    link_atendimento_online VARCHAR(255),
    data_criacao TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    data_atualizacao TIMESTAMP WITHOUT TIME ZONE,
    FOREIGN KEY (paciente_id) REFERENCES tb_paciente (usuario_id) ON DELETE CASCADE,
    FOREIGN KEY (medico_id) REFERENCES tb_medico (usuario_id) ON DELETE CASCADE
    -- Adicionar UNIQUE constraint se necessário (ex: paciente não pode ter duas consultas no mesmo horário)
    -- UNIQUE (paciente_id, data_hora_consulta),
    -- UNIQUE (medico_id, data_hora_consulta) -- Cuidado com status cancelados
);

-- Índices podem ser adicionados para otimizar consultas
CREATE INDEX idx_usuario_email ON tb_usuario (email);
CREATE INDEX idx_consulta_paciente_data ON tb_consulta (paciente_id, data_hora_consulta);
CREATE INDEX idx_consulta_medico_data ON tb_consulta (medico_id, data_hora_consulta);
CREATE INDEX idx_disponibilidade_medico_dia ON tb_disponibilidade_medico (medico_id, dia_semana, ativo);

-- Você pode adicionar um script V2__insert_initial_data.sql para popular perfis iniciais, por exemplo:
-- INSERT INTO tb_perfil (nome) VALUES ('ROLE_PACIENTE');
-- INSERT INTO tb_perfil (nome) VALUES ('ROLE_MEDICO');
-- INSERT INTO tb_perfil (nome) VALUES ('ROLE_ADMIN');

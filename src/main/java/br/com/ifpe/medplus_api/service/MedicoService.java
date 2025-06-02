package br.com.ifpe.medplus_api.service;

import br.com.ifpe.medplus_api.dto.EnderecoRequest;
import br.com.ifpe.medplus_api.dto.MedicoRequest;
import br.com.ifpe.medplus_api.model.acesso.Perfil;
import br.com.ifpe.medplus_api.model.acesso.PerfilEnum;
import br.com.ifpe.medplus_api.model.common.Endereco;
import br.com.ifpe.medplus_api.model.medico.Medico;
import br.com.ifpe.medplus_api.repository.MedicoRepository;
import br.com.ifpe.medplus_api.repository.PerfilRepository;
import br.com.ifpe.medplus_api.repository.UsuarioRepository;
import br.com.ifpe.medplus_api.util.exception.EntidadeNaoEncontradaException;
import jakarta.persistence.EntityExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Serviço para gerenciar operações relacionadas a Médicos.
 */
@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository; // Para verificar duplicidade de email/cpf
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService; // Opcional, para notificações

    public MedicoService(MedicoRepository medicoRepository,
                         UsuarioRepository usuarioRepository,
                         PerfilRepository perfilRepository,
                         PasswordEncoder passwordEncoder,
                         EmailService emailService) {
        this.medicoRepository = medicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Registra um novo médico no sistema.
     *
     * @param request DTO com os dados do médico.
     * @return O médico salvo.
     * @throws EntityExistsException Se o email, CPF ou CRM já estiverem em uso.
     * @throws EntidadeNaoEncontradaException Se o perfil de médico não for encontrado.
     */
    @Transactional
    public Medico registrarMedico(MedicoRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EntityExistsException("Email já cadastrado no sistema: " + request.getEmail());
        }
        if (usuarioRepository.existsByCpf(request.getCpf())) {
            throw new EntityExistsException("CPF já cadastrado no sistema: " + request.getCpf());
        }
        if (medicoRepository.existsByCrm(request.getCrm())) {
            throw new EntityExistsException("CRM já cadastrado no sistema: " + request.getCrm());
        }

        Medico medico = new Medico();
        medico.setNome(request.getNome());
        medico.setEmail(request.getEmail());
        medico.setSenha(passwordEncoder.encode(request.getSenha()));
        medico.setCpf(request.getCpf());
        medico.setDataNascimento(request.getDataNascimento());
        medico.setTelefone(request.getTelefone());
        medico.setCrm(request.getCrm());
        medico.setEspecialidade(request.getEspecialidade());
        medico.setAtivo(true); // Médicos podem precisar de aprovação, mas por padrão ativo aqui

        if (request.getEndereco() != null) {
            EnderecoRequest endReq = request.getEndereco();
            Endereco endereco = new Endereco(endReq.getLogradouro(), endReq.getNumero(), endReq.getComplemento(),
                    endReq.getBairro(), endReq.getCidade(), endReq.getUf(), endReq.getCep());
            medico.setEndereco(endereco);
        }

        Perfil perfilMedico = perfilRepository.findByNome(PerfilEnum.ROLE_MEDICO)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Perfil 'MEDICO' não encontrado."));
        Set<Perfil> perfis = new HashSet<>();
        perfis.add(perfilMedico);
        medico.setPerfis(perfis);

        Medico medicoSalvo = medicoRepository.save(medico);

        // Enviar email de boas-vindas ou notificação (opcional)
        emailService.sendWelcomeEmail(medicoSalvo.getNome(), medicoSalvo.getEmail());

        return medicoSalvo;
    }

    /**
     * Busca um médico pelo ID.
     *
     * @param id O ID do médico.
     * @return O médico encontrado.
     * @throws EntidadeNaoEncontradaException Se o médico não for encontrado.
     */
    @Transactional(readOnly = true)
    public Medico buscarPorId(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Médico não encontrado com ID: " + id));
    }
    
    /**
     * Busca um médico pelo email.
     *
     * @param email O email do médico.
     * @return O médico encontrado.
     * @throws EntidadeNaoEncontradaException Se o médico não for encontrado.
     */
    @Transactional(readOnly = true)
    public Medico buscarPorEmail(String email) {
        return medicoRepository.findByEmail(email)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Médico não encontrado com email: " + email));
    }

    /**
     * Lista todos os médicos.
     * (Considerar paginação para grandes volumes de dados)
     * @return Lista de todos os médicos.
     */
    @Transactional(readOnly = true)
    public List<Medico> listarTodos() {
        return medicoRepository.findAll();
    }

    /**
     * Lista todos os médicos ativos.
     * @return Lista de médicos ativos.
     */
    @Transactional(readOnly = true)
    public List<Medico> listarAtivos() {
        return medicoRepository.findAllByAtivoTrue();
    }

    /**
     * Lista médicos por especialidade.
     * @param especialidade A especialidade a ser buscada.
     * @return Lista de médicos da especialidade.
     */
    @Transactional(readOnly = true)
    public List<Medico> listarPorEspecialidade(String especialidade) {
        return medicoRepository.findByEspecialidadeIgnoreCase(especialidade);
    }


    /**
     * Atualiza os dados de um médico.
     *
     * @param id      O ID do médico a ser atualizado.
     * @param request DTO com os novos dados do médico.
     * @return O médico atualizado.
     * @throws EntidadeNaoEncontradaException Se o médico não for encontrado.
     * @throws EntityExistsException Se o novo email, CPF ou CRM já pertencer a outro usuário/médico.
     */
    @Transactional
    public Medico atualizarMedico(Long id, MedicoRequest request) {
        Medico medico = buscarPorId(id);

        if (!medico.getEmail().equals(request.getEmail()) && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EntityExistsException("Email " + request.getEmail() + " já está em uso por outro usuário.");
        }
        if (!medico.getCpf().equals(request.getCpf()) && usuarioRepository.existsByCpf(request.getCpf())) {
            throw new EntityExistsException("CPF " + request.getCpf() + " já está em uso por outro usuário.");
        }
        if (!medico.getCrm().equals(request.getCrm()) && medicoRepository.existsByCrm(request.getCrm())) {
            throw new EntityExistsException("CRM " + request.getCrm() + " já está em uso por outro médico.");
        }

        medico.setNome(request.getNome());
        medico.setEmail(request.getEmail());
        medico.setCpf(request.getCpf());
        medico.setDataNascimento(request.getDataNascimento());
        medico.setTelefone(request.getTelefone());
        medico.setCrm(request.getCrm());
        medico.setEspecialidade(request.getEspecialidade());
        // A senha não é atualizada aqui. Deve haver um método específico para isso.

        if (request.getEndereco() != null) {
            EnderecoRequest endReq = request.getEndereco();
            Endereco endereco = new Endereco(endReq.getLogradouro(), endReq.getNumero(), endReq.getComplemento(),
                    endReq.getBairro(), endReq.getCidade(), endReq.getUf(), endReq.getCep());
            medico.setEndereco(endereco);
        } else {
            medico.setEndereco(null);
        }

        return medicoRepository.save(medico);
    }

    /**
     * Desativa um médico.
     *
     * @param id O ID do médico a ser desativado.
     * @throws EntidadeNaoEncontradaException Se o médico não for encontrado.
     */
    @Transactional
    public void desativarMedico(Long id) {
        Medico medico = buscarPorId(id);
        medico.setAtivo(false);
        // Adicionar lógica para tratar agendas futuras, notificar pacientes, etc.
        medicoRepository.save(medico);
    }

    /**
     * Ativa um médico.
     *
     * @param id O ID do médico a ser ativado.
     * @throws EntidadeNaoEncontradaException Se o médico não for encontrado.
     */
    @Transactional
    public void ativarMedico(Long id) {
        Medico medico = buscarPorId(id);
        medico.setAtivo(true);
        medicoRepository.save(medico);
    }
    
    /**
     * Exclui fisicamente um médico (USAR COM CUIDADO).
     * Geralmente, a desativação (exclusão lógica) é preferível.
     *
     * @param id O ID do médico a ser excluído.
     * @throws EntidadeNaoEncontradaException Se o médico não for encontrado.
     */
    @Transactional
    public void excluirMedicoFisicamente(Long id) {
        if (!medicoRepository.existsById(id)) {
            throw new EntidadeNaoEncontradaException("Médico não encontrado com ID: " + id + " para exclusão.");
        }
        // Adicionar lógica para tratar dependências (consultas, disponibilidades, etc.) antes de excluir.
        medicoRepository.deleteById(id);
    }
}





package br.com.ifpe.medplus_api.service;

import br.com.ifpe.medplus_api.dto.EnderecoRequest;
import br.com.ifpe.medplus_api.dto.PacienteRequest;
import br.com.ifpe.medplus_api.model.acesso.Perfil;
import br.com.ifpe.medplus_api.model.acesso.PerfilEnum;
import br.com.ifpe.medplus_api.model.common.Endereco;
import br.com.ifpe.medplus_api.model.paciente.Paciente;
import br.com.ifpe.medplus_api.repository.PacienteRepository;
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
 * Serviço para gerenciar operações relacionadas a Pacientes.
 */
@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    
    public PacienteService(PacienteRepository pacienteRepository,
                           UsuarioRepository usuarioRepository,
                           PerfilRepository perfilRepository,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Registra um novo paciente no sistema.
     *
     * @param request DTO com os dados do paciente.
     * @return O paciente salvo.
     * @throws EntityExistsException Se o email ou CPF já estiverem em uso.
     * @throws EntidadeNaoEncontradaException Se o perfil de paciente não for encontrado.
     */
    @Transactional
public Paciente registrarPaciente(PacienteRequest request) {
    if (usuarioRepository.existsByEmail(request.getEmail())) {
        throw new EntityExistsException("Email já cadastrado no sistema: " + request.getEmail());
    }
    if (usuarioRepository.existsByCpf(request.getCpf())) {
        throw new EntityExistsException("CPF já cadastrado no sistema: " + request.getCpf());
    }

    Paciente paciente = new Paciente();
    paciente.setNome(request.getNome());
    paciente.setEmail(request.getEmail());
    paciente.setSenha(passwordEncoder.encode(request.getSenha()));
    paciente.setCpf(request.getCpf());
    paciente.setDataNascimento(request.getDataNascimento());
    paciente.setTelefone(request.getTelefone());
    paciente.setHistoricoMedico(request.getHistoricoMedico());
    paciente.setAtivo(true);

    if (request.getEndereco() != null) {
        EnderecoRequest endReq = request.getEndereco();
        Endereco endereco = new Endereco(endReq.getLogradouro(), endReq.getNumero(), endReq.getComplemento(),
                endReq.getBairro(), endReq.getCidade(), endReq.getUf(), endReq.getCep());
        paciente.setEndereco(endereco);
    }

    Perfil perfilPaciente = perfilRepository.findByNome(PerfilEnum.ROLE_PACIENTE)
            .orElseThrow(() -> new EntidadeNaoEncontradaException("Perfil 'PACIENTE' não encontrado."));
    Set<Perfil> perfis = new HashSet<>();
    perfis.add(perfilPaciente);
    
    // --- CORREÇÃO APLICADA AQUI ---
    paciente.setPerfis(perfis); 

        Paciente pacienteSalvo = pacienteRepository.save(paciente);
    
        emailService.sendWelcomeEmail(pacienteSalvo.getNome(), pacienteSalvo.getEmail());

        return pacienteSalvo;
    }

    /**
     * Busca um paciente pelo ID.
     *
     * @param id O ID do paciente.
     * @return O paciente encontrado.
     * @throws EntidadeNaoEncontradaException Se o paciente não for encontrado.
     */
    @Transactional(readOnly = true)
    public Paciente buscarPorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Paciente não encontrado com ID: " + id));
    }
    
    /**
     * Busca um paciente pelo email.
     *
     * @param email O email do paciente.
     * @return O paciente encontrado.
     * @throws EntidadeNaoEncontradaException Se o paciente não for encontrado.
     */
    @Transactional(readOnly = true)
    public Paciente buscarPorEmail(String email) {
        return pacienteRepository.findByEmail(email)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Paciente não encontrado com email: " + email));
    }

    /**
     * Lista todos os pacientes.
     * (Considerar paginação para grandes volumes de dados)
     * @return Lista de todos os pacientes.
     */
    @Transactional(readOnly = true)
    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }

    /**
     * Atualiza os dados de um paciente.
     *
     * @param id      O ID do paciente a ser atualizado.
     * @param request DTO com os novos dados do paciente.
     * @return O paciente atualizado.
     * @throws EntidadeNaoEncontradaException Se o paciente não for encontrado.
     * @throws EntityExistsException Se o novo email ou CPF já pertencer a outro usuário.
     */
    @Transactional
    public Paciente atualizarPaciente(Long id, PacienteRequest request) {
        Paciente paciente = buscarPorId(id);

        // Verifica se o novo email já existe em outro usuário
        if (!paciente.getEmail().equals(request.getEmail()) && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EntityExistsException("Email " + request.getEmail() + " já está em uso por outro usuário.");
        }
        // Verifica se o novo CPF já existe em outro usuário
        if (!paciente.getCpf().equals(request.getCpf()) && usuarioRepository.existsByCpf(request.getCpf())) {
            throw new EntityExistsException("CPF " + request.getCpf() + " já está em uso por outro usuário.");
        }

        paciente.setNome(request.getNome());
        paciente.setEmail(request.getEmail());
        paciente.setCpf(request.getCpf());
        paciente.setDataNascimento(request.getDataNascimento());
        paciente.setTelefone(request.getTelefone());
        paciente.setHistoricoMedico(request.getHistoricoMedico());

        if (request.getEndereco() != null) {
            EnderecoRequest endReq = request.getEndereco();
            Endereco endereco = new Endereco(endReq.getLogradouro(), endReq.getNumero(), endReq.getComplemento(),
                    endReq.getBairro(), endReq.getCidade(), endReq.getUf(), endReq.getCep());
            paciente.setEndereco(endereco);
        } else {
            paciente.setEndereco(null); // Permite remover o endereço
        }

        return pacienteRepository.save(paciente);
    }

    /**
     * Desativa um paciente (exclusão lógica).
     *
     * @param id O ID do paciente a ser desativado.
     * @throws EntidadeNaoEncontradaException Se o paciente não for encontrado.
     */
    @Transactional
    public void desativarPaciente(Long id) {
        Paciente paciente = buscarPorId(id);
        paciente.setAtivo(false);
        // Adicionar lógica para cancelar consultas futuras, etc.
        pacienteRepository.save(paciente);
    }
    
    // MÉTODO ADICIONADO PARA CORRIGIR A FUNCIONALIDADE DO ADMINCONTROLLER
    /**
     * Ativa um paciente previamente desativado.
     *
     * @param id O ID do paciente a ser ativado.
     * @throws EntidadeNaoEncontradaException Se o paciente não for encontrado.
     */
    @Transactional
    public void ativarPaciente(Long id) {
        Paciente paciente = buscarPorId(id);
        paciente.setAtivo(true);
        pacienteRepository.save(paciente);
    }
    
    /**
     * Exclui fisicamente um paciente (USAR COM CUIDADO).
     *
     * @param id O ID do paciente a ser excluído.
     * @throws EntidadeNaoEncontradaException Se o paciente não for encontrado.
     */
    @Transactional
    public void excluirPacienteFisicamente(Long id) {
        if (!pacienteRepository.existsById(id)) {
            throw new EntidadeNaoEncontradaException("Paciente não encontrado com ID: " + id + " para exclusão.");
        }
        pacienteRepository.deleteById(id);
    }
}


// Fim do código


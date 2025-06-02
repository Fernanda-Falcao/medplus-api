package br.com.ifpe.medplus_api.service;

import br.com.ifpe.medplus_api.dto.AdminRequest;
import br.com.ifpe.medplus_api.dto.EnderecoRequest;
import br.com.ifpe.medplus_api.model.acesso.Perfil;
import br.com.ifpe.medplus_api.model.acesso.PerfilEnum;
import br.com.ifpe.medplus_api.model.admin.Admin;
import br.com.ifpe.medplus_api.model.common.Endereco;
import br.com.ifpe.medplus_api.repository.AdminRepository;
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
 * Serviço para gerenciar operações relacionadas a Administradores.
 */
@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final UsuarioRepository usuarioRepository; // Para verificar duplicidade de email/cpf
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    //private final EmailService emailService; // Opcional, para notificações

    
    public AdminService(AdminRepository adminRepository,
                        UsuarioRepository usuarioRepository,
                        PerfilRepository perfilRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService) {
        this.adminRepository = adminRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        //this.emailService = emailService;
    }

    /**
     * Registra um novo administrador no sistema.
     *
     * @param request DTO com os dados do administrador.
     * @return O administrador salvo.
     * @throws EntityExistsException          Se o email ou CPF já estiverem em uso.
     * @throws EntidadeNaoEncontradaException Se o perfil de administrador não for encontrado.
     */
    @Transactional
    public Admin registrarAdmin(AdminRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EntityExistsException("Email já cadastrado no sistema: " + request.getEmail());
        }
        if (usuarioRepository.existsByCpf(request.getCpf())) {
            throw new EntityExistsException("CPF já cadastrado no sistema: " + request.getCpf());
        }

        Admin admin = new Admin();
        admin.setNome(request.getNome());
        admin.setEmail(request.getEmail());
        admin.setSenha(passwordEncoder.encode(request.getSenha()));
        admin.setCpf(request.getCpf());
        admin.setDataNascimento(request.getDataNascimento());
        admin.setTelefone(request.getTelefone());
        admin.setNivelAcesso(request.getNivelAcesso());
        admin.setAtivo(true);

        if (request.getEndereco() != null) {
            EnderecoRequest endReq = request.getEndereco();
            Endereco endereco = new Endereco(endReq.getLogradouro(), endReq.getNumero(), endReq.getComplemento(),
                    endReq.getBairro(), endReq.getCidade(), endReq.getUf(), endReq.getCep());
            admin.setEndereco(endereco);
        }

        Perfil perfilAdmin = perfilRepository.findByNome(PerfilEnum.ROLE_ADMIN)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Perfil 'ADMIN' não encontrado."));
        Set<Perfil> perfis = new HashSet<>();
        perfis.add(perfilAdmin);
        admin.setPerfis(perfis);

        Admin adminSalvo = adminRepository.save(admin);

        // Enviar email de boas-vindas ou notificação (opcional)
        // emailService.sendWelcomeEmail(adminSalvo.getNome(), adminSalvo.getEmail());

        return adminSalvo;
    }

    /**
     * Busca um administrador pelo ID.
     *
     * @param id O ID do administrador.
     * @return O administrador encontrado.
     * @throws EntidadeNaoEncontradaException Se o administrador não for encontrado.
     */
    @Transactional(readOnly = true)
    public Admin buscarPorId(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Administrador não encontrado com ID: " + id));
    }
    
    /**
     * Busca um administrador pelo email.
     *
     * @param email O email do administrador.
     * @return O administrador encontrado.
     * @throws EntidadeNaoEncontradaException Se o administrador não for encontrado.
     */
    @Transactional(readOnly = true)
    public Admin buscarPorEmail(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Administrador não encontrado com email: " + email));
    }

    /**
     * Lista todos os administradores.
     *
     * @return Lista de todos os administradores.
     */
    @Transactional(readOnly = true)
    public List<Admin> listarTodos() {
        return adminRepository.findAll();
    }

    /**
     * Atualiza os dados de um administrador.
     *
     * @param id      O ID do administrador a ser atualizado.
     * @param request DTO com os novos dados do administrador.
     * @return O administrador atualizado.
     * @throws EntidadeNaoEncontradaException Se o administrador não for encontrado.
     * @throws EntityExistsException          Se o novo email ou CPF já pertencer a outro usuário.
     */
    @Transactional
    public Admin atualizarAdmin(Long id, AdminRequest request) {
        Admin admin = buscarPorId(id);

        if (!admin.getEmail().equals(request.getEmail()) && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EntityExistsException("Email " + request.getEmail() + " já está em uso por outro usuário.");
        }
        if (!admin.getCpf().equals(request.getCpf()) && usuarioRepository.existsByCpf(request.getCpf())) {
            throw new EntityExistsException("CPF " + request.getCpf() + " já está em uso por outro usuário.");
        }

        admin.setNome(request.getNome());
        admin.setEmail(request.getEmail());
        admin.setCpf(request.getCpf());
        admin.setDataNascimento(request.getDataNascimento());
        admin.setTelefone(request.getTelefone());
        admin.setNivelAcesso(request.getNivelAcesso());
        // A senha não é atualizada aqui. Deve haver um método específico para isso.

        if (request.getEndereco() != null) {
            EnderecoRequest endReq = request.getEndereco();
            Endereco endereco = new Endereco(endReq.getLogradouro(), endReq.getNumero(), endReq.getComplemento(),
                    endReq.getBairro(), endReq.getCidade(), endReq.getUf(), endReq.getCep());
            admin.setEndereco(endereco);
        } else {
            admin.setEndereco(null);
        }

        return adminRepository.save(admin);
    }

    /**
     * Desativa um administrador.
     *
     * @param id O ID do administrador a ser desativado.
     * @throws EntidadeNaoEncontradaException Se o administrador não for encontrado.
     */
    @Transactional
    public void desativarAdmin(Long id) {
        Admin admin = buscarPorId(id);
        admin.setAtivo(false);
        adminRepository.save(admin);
    }
    
    /**
     * Ativa um administrador.
     *
     * @param id O ID do administrador a ser ativado.
     * @throws EntidadeNaoEncontradaException Se o administrador não for encontrado.
     */
    @Transactional
    public void ativarAdmin(Long id) {
        Admin admin = buscarPorId(id);
        admin.setAtivo(true);
        adminRepository.save(admin);
    }

    /**
     * Exclui fisicamente um administrador (USAR COM CUIDADO).
     * Geralmente, a desativação (exclusão lógica) é preferível, especialmente para administradores.
     *
     * @param id O ID do administrador a ser excluído.
     * @throws EntidadeNaoEncontradaException Se o administrador não for encontrado.
     */
    @Transactional
    public void excluirAdminFisicamente(Long id) {
        if (!adminRepository.existsById(id)) {
            throw new EntidadeNaoEncontradaException("Administrador não encontrado com ID: " + id + " para exclusão.");
        }
        // Adicionar verificações de segurança antes de excluir um admin,
        // como não permitir a exclusão do último admin ativo, etc.
        adminRepository.deleteById(id);
    }

    // Métodos de gerenciamento de outros usuários (Pacientes, Médicos) pelo Admin:
    // - Listar todos os usuários
    // - Ativar/Desativar contas de usuários
    // - Mudar perfil de usuário
    // - Resetar senha de usuário
    // Estes métodos seriam implementados aqui, utilizando os respectivos repositórios e serviços.
}




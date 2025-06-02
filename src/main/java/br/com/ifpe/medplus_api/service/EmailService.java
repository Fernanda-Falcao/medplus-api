package br.com.ifpe.medplus_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Serviço para envio de emails.
 * Esta é uma implementação básica/placeholder. Para produção, configure
 * um provedor de email (SMTP, SendGrid, AWS SES, etc.) no application.properties
 * e ajuste este serviço conforme necessário.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@medplus.com}") // Email remetente padrão
    private String fromEmail;


    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envia um email de forma síncrona.
     *
     * @param to      Destinatário do email.
     * @param subject Assunto do email.
     * @param text    Conteúdo do email.
     */
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("Email enviado para {} com assunto: {}", to, subject);
        } catch (MailException e) {
            logger.error("Erro ao enviar email para {}: {}", to, e.getMessage());
            // Tratar a exceção (ex: logar, tentar novamente, notificar admin)
        }
    }

    /**
     * Envia um email de forma assíncrona.
     * Para usar @Async, você precisa habilitar o suporte a @Async na sua aplicação
     * (ex: com @EnableAsync em uma classe de configuração).
     *
     * @param to      Destinatário do email.
     * @param subject Assunto do email.
     * @param text    Conteúdo do email.
     */
    @Async
    public void sendSimpleMessageAsync(String to, String subject, String text) {
        logger.info("Tentando enviar email assíncrono para {}...", to);
        sendSimpleMessage(to, subject, text);
    }

    // Exemplos de métodos mais específicos:

    /**
     * Envia um email de boas-vindas para um novo usuário.
     * @param nomeUsuario Nome do usuário.
     * @param emailUsuario Email do usuário.
     */
    @Async
    public void sendWelcomeEmail(String nomeUsuario, String emailUsuario) {
        String subject = "Bem-vindo(a) à MedPlus!";
        String text = String.format("Olá %s,\n\nSeu cadastro na MedPlus foi realizado com sucesso!\n\n" +
                "Aproveite nossos serviços de agendamento online.\n\nAtenciosamente,\nEquipe MedPlus", nomeUsuario);
        sendSimpleMessage(emailUsuario, subject, text);
    }

    /**
     * Envia um email de confirmação de consulta.
     * @param emailPaciente Email do paciente.
     * @param nomePaciente Nome do paciente.
     * @param nomeMedico Nome do médico.
     * @param dataHoraConsulta Data e hora da consulta.
     */
    @Async
    public void sendConsultaAgendadaEmail(String emailPaciente, String nomePaciente, String nomeMedico, String dataHoraConsulta) {
        String subject = "Confirmação de Agendamento de Consulta - MedPlus";
        String text = String.format("Olá %s,\n\nSua consulta com Dr(a). %s foi agendada para %s.\n\n" +
                "Não se esqueça!\n\nAtenciosamente,\nEquipe MedPlus", nomePaciente, nomeMedico, dataHoraConsulta);
        sendSimpleMessage(emailPaciente, subject, text);
    }

    /**
     * Envia um email para recuperação de senha.
     * @param emailUsuario Email do usuário.
     * @param tokenRedefinicao Token para redefinição de senha.
     * @param urlRedefinicao URL base para a página de redefinição.
     */
    @Async
    public void sendPasswordResetEmail(String emailUsuario, String tokenRedefinicao, String urlRedefinicao) {
        String subject = "Redefinição de Senha - MedPlus";
        String link = urlRedefinicao + "?token=" + tokenRedefinicao; // Exemplo de link
        String text = String.format("Olá,\n\nVocê solicitou a redefinição de sua senha na MedPlus.\n\n" +
                "Clique no link a seguir para criar uma nova senha: %s\n\n" +
                "Se você não solicitou esta alteração, por favor, ignore este email.\n\n" +
                "Atenciosamente,\nEquipe MedPlus", link);
        sendSimpleMessage(emailUsuario, subject, text);
    }
}


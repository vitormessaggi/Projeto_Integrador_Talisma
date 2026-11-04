package br.com.talisma.api.imoveisapi.identidade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class EnvioCodigo {
    private static final Logger LOG = LoggerFactory.getLogger(EnvioCodigo.class);
    private final JavaMailSender smtp;
    private final String remetente;

    EnvioCodigo(JavaMailSender smtp, @Value("${APP_MAIL_FROM:no-reply@talisma.local}") String remetente) {
        this.smtp = smtp;
        this.remetente = remetente;
    }

    @TransactionalEventListener
    void enviar(IdentidadeService.CodigoEmail evento) {
        var mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(evento.email());
        mensagem.setSubject(evento.finalidade().equals("CADASTRO") ? "Verifique seu e-mail" : "Recupere sua senha");
        mensagem.setText("Seu código Talismã Imóveis é " + evento.codigo() + ". Ele expira em 10 minutos.");
        try {
            smtp.send(mensagem);
        } catch (MailException erro) {
            LOG.warn("Falha ao enviar código de {}. O usuário pode solicitar outro código.", evento.finalidade());
        }
    }
}

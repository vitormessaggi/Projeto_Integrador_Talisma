package br.com.talisma.api.imoveisapi.identidade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.talisma.api.imoveisapi.email.EmailTalisma;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class EnvioCodigo {
    private static final Logger LOG = LoggerFactory.getLogger(EnvioCodigo.class);
    private final EmailTalisma email;

    EnvioCodigo(EmailTalisma email) {
        this.email = email;
    }

    @TransactionalEventListener
    void enviar(IdentidadeService.CodigoEmail evento) {
        String assunto = evento.finalidade().equals("CADASTRO") ? "Verifique seu e-mail" : "Recupere sua senha";
        try {
            email.enviar(evento.email(), assunto, "Seu código Talismã Imóveis é " + evento.codigo() + ". Ele expira em 10 minutos.");
        } catch (MailException erro) {
            LOG.warn("Falha ao enviar código de {}. O usuário pode solicitar outro código.", evento.finalidade());
        }
    }
}

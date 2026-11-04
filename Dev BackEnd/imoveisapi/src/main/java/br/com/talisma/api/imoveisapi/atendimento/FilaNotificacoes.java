package br.com.talisma.api.imoveisapi.atendimento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class FilaNotificacoes {
    private final JdbcClient banco;
    private final JavaMailSender smtp;
    private final String remetente;

    FilaNotificacoes(JdbcClient banco, JavaMailSender smtp,
                     @Value("${APP_MAIL_FROM:no-reply@talisma.local}") String remetente) {
        this.banco = banco;
        this.smtp = smtp;
        this.remetente = remetente;
    }

    record Pendente(long id, String email, String assunto, String corpo, int tentativas) {}

    @Scheduled(fixedDelayString = "${APP_NOTIFICACOES_INTERVALO:5000}")
    @Transactional
    public void processar() {
        var item = banco.sql("""
                SELECT n.id,u.email,n.assunto,n.corpo,n.tentativas FROM notificacao n JOIN usuario u ON u.id=n.usuario_id
                WHERE n.status='PENDENTE' AND n.proxima_tentativa<=UTC_TIMESTAMP(6)
                ORDER BY n.proxima_tentativa,n.id LIMIT 1 FOR UPDATE OF n SKIP LOCKED
                """).query(Pendente.class).optional().orElse(null);
        if (item == null) {
            return;
        }
        var mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(item.email());
        mensagem.setSubject(item.assunto());
        mensagem.setText(item.corpo());
        try {
            smtp.send(mensagem);
            banco.sql("UPDATE notificacao SET status='ENVIADA', enviada_em=UTC_TIMESTAMP(6) WHERE id=?")
                    .param(item.id()).update();
        } catch (MailException erro) {
            banco.sql("""
                    UPDATE notificacao SET tentativas=?,status=?,proxima_tentativa=UTC_TIMESTAMP(6)+INTERVAL 1 MINUTE
                    WHERE id=?
                    """).params(item.tentativas() + 1, item.tentativas() >= 4 ? "FALHOU" : "PENDENTE", item.id()).update();
        }
    }
}

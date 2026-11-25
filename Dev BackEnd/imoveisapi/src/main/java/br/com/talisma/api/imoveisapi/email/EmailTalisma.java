package br.com.talisma.api.imoveisapi.email;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class EmailTalisma {
    private final JavaMailSender smtp;
    private final String remetente;

    public EmailTalisma(JavaMailSender smtp, @Value("${talisma.mail.from:${APP_MAIL_FROM:no-reply@talisma.local}}") String remetente) {
        this.smtp = smtp;
        this.remetente = remetente;
    }

    public void enviar(String destino, String assunto, String texto) {
        String html = """
                <html lang="pt-BR"><body style="margin:0;background:#fcfbfa;color:#181317;font-family:Poppins,Arial,sans-serif">
                <div style="max-width:560px;margin:24px auto;background:#fff;border:1px solid #e3dbe0">
                <div style="background:#8d001a;color:#fff;padding:20px 28px;font-weight:700">Talismã Imóveis</div>
                <div style="padding:28px"><h1 style="font-size:22px;color:#35000b;margin:0 0 16px">%s</h1>
                <p style="font-size:16px;line-height:1.6;margin:0">%s</p></div></div></body></html>
                """.formatted(HtmlUtils.htmlEscape(assunto, "UTF-8"), HtmlUtils.htmlEscape(texto, "UTF-8").lines()
                        .collect(java.util.stream.Collectors.joining("<br>")));
        smtp.send(message -> {
            var email = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            email.setFrom(remetente, "Talismã Imóveis");
            email.setTo(destino);
            email.setSubject(assunto);
            email.setText(texto, html);
        });
    }
}

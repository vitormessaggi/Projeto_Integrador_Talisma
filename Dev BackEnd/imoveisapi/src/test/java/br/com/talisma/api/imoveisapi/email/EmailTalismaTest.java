package br.com.talisma.api.imoveisapi.email;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.*;

class EmailTalismaTest {
    @Test
    void incluiTextoEHtmlComConteudoEscapado() throws Exception {
        class Captura extends JavaMailSenderImpl {
            MimeMessage recebido;
            @Override protected void doSend(MimeMessage[] mensagens, Object[] originais) {
                recebido = mensagens[0];
            }
        }
        var smtp = new Captura();
        new EmailTalisma(smtp, "contas@example.org").enviar("cliente@example.net", "Confirmação", "Código 123456 <teste>");
        var mensagem = smtp.recebido;
        mensagem.saveChanges();
        assertEquals("Talismã Imóveis", ((InternetAddress) mensagem.getFrom()[0]).getPersonal());
        assertEquals("cliente@example.net", mensagem.getAllRecipients()[0].toString());
        assertEquals("Confirmação", mensagem.getSubject());
        assertEquals("Código 123456 <teste>", parte(mensagem, "text/plain"));
        String html = parte(mensagem, "text/html");
        assertTrue(html.contains("Código 123456 &lt;teste&gt;"));
        assertTrue(html.contains("#8d001a"));
    }

    private static String parte(Part parte, String tipo) throws Exception {
        if (parte.isMimeType(tipo)) return (String) parte.getContent();
        if (parte.getContent() instanceof Multipart partes) {
            for (int i = 0; i < partes.getCount(); i++) {
                String texto = parte(partes.getBodyPart(i), tipo);
                if (texto != null) return texto;
            }
        }
        return null;
    }
}

package br.com.talisma.imoveis.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Máscara enquanto o usuário digita (6.6, D12, D30): CPF "000.000.000-00" e telefone "(00) 00000-0000"
 * (ou "(00) 0000-0000" com 10 dígitos). Uso: {@code MascaraTexto.cpf(edtCpf)} / {@code telefone(edtTelefone)}.
 */
public final class MascaraTexto {

    private MascaraTexto() {
    }

    public static void cpf(EditText campo) {
        aplicar(campo, false);
    }

    public static void telefone(EditText campo) {
        aplicar(campo, true);
    }

    private static void aplicar(EditText campo, boolean telefone) {
        campo.addTextChangedListener(new TextWatcher() {
            private boolean editando;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (editando) {
                    return;
                }
                editando = true;
                String formatado = telefone ? Formatos.telefone(s.toString()) : Formatos.cpf(s.toString());
                if (!formatado.equals(s.toString())) {
                    s.replace(0, s.length(), formatado);
                }
                editando = false;
            }
        });
    }
}

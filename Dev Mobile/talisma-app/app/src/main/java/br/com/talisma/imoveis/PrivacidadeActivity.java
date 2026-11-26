package br.com.talisma.imoveis;

import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import br.com.talisma.imoveis.dados.*;
import br.com.talisma.imoveis.util.Tarefas;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

/** Documentos offline versionados, solicitações autenticadas e exportação pelo seletor do Android. */
public class PrivacidadeActivity extends AppCompatActivity {
    private LinearLayout corpo;
    private TextView resultado;
    private EditText senha;
    private String exportacao;
    private boolean ocupado;
    private final ActivityResultLauncher<String> salvar = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri == null || exportacao == null) { exportacao=null; return; }
                final String conteudo=exportacao; exportacao=null;
                Tarefas.executar(() -> {
                    try (var out=getContentResolver().openOutputStream(uri)) {
                        if (out==null) throw new java.io.IOException("Arquivo indisponível");
                        out.write(conteudo.getBytes(StandardCharsets.UTF_8));
                    }
                    catch(java.io.IOException e){throw new ApiException(500,"Não foi possível salvar o arquivo.",e);}
                    return "Cópia salva no arquivo escolhido. Guarde-a com segurança.";
                }, retorno());
            });
    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        ScrollView scroll=new ScrollView(this); corpo=new LinearLayout(this); corpo.setOrientation(LinearLayout.VERTICAL);
        int margem=(int)(24*getResources().getDisplayMetrics().density); corpo.setPadding(margem,margem,margem,margem); scroll.addView(corpo); setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{
            var barras=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left,barras.top,barras.right,barras.bottom); return insets;
        });
        texto("Termos e privacidade",24);
        botao("Voltar",this::finish);
        botao("Ler Termos de Uso",()->documento("termos"));
        botao("Ler Política de Privacidade",()->documento("privacidade"));
        texto("Demonstração acadêmica. Use dados fictícios. O aceite dos termos não autoriza rastreamento. Versão 2026-11-25.",16);
        resultado=texto("",16);
        if (!Sessao.logado(this)) { texto("Entre na sua conta para solicitar acesso ou exclusão dos dados.",16); return; }
        if (Preferencias.modoDemonstracao(this)) { texto("O modo simulado não cria conta na AWS. Para testar os direitos do titular no servidor, desligue o modo demonstração e entre com uma conta da API. Você pode apagar os dados locais abaixo.",16);
            botao("Apagar dados simulados deste aparelho",()->new MaterialAlertDialogBuilder(this).setMessage("Apagar contas e dados fictícios do modo simulado deste aparelho?").setNegativeButton("Cancelar",null).setPositiveButton("Apagar",(d,w)->{
                new FavoritoRepository(this).limparParaExclusao();
                var db=BancoLocal.instancia(this).getWritableDatabase();
                for(String tabela:new String[]{BancoLocal.SIM_VISITA,BancoLocal.SIM_FAVORITO,BancoLocal.SIM_USUARIO,BancoLocal.SIM_FOTO}) db.delete(tabela,null,null);
                Sessao.limpar(this); finish();
            }).show()); return;
        }
        CheckBox aceite=new CheckBox(this); aceite.setText("Aceito os termos e confirmo a leitura da política acima"); corpo.addView(aceite);
        botao("Registrar aceite atual",()->{if(!aceite.isChecked()){resultado.setText("Leia os documentos e marque a confirmação.");return;} executar("ACEITAR");});
        var campoSenha = new TextInputLayout(this);
        campoSenha.setHint("Senha atual para confirmar identidade");
        senha = new TextInputEditText(campoSenha.getContext());
        senha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        campoSenha.addView(senha);
        campoSenha.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        corpo.addView(campoSenha);
        botao("Solicitar acesso",()->executar("ACESSO"));
        botao("Baixar meus dados",()->executar("EXPORTAR"));
        botao("Minhas solicitações",()->executar("LISTAR"));
        texto("Excluir uma conta de cliente remove documentos legados, contatos, visitas e favoritos e encerra suas sessões. Contas internas seguem para análise administrativa. Backups expiram em sete dias.",16);
        botao("Solicitar exclusão dos meus dados",()->new MaterialAlertDialogBuilder(this).setTitle("Confirmar exclusão")
                .setMessage("Deseja excluir sua conta e os dados relacionados? Esta operação não pode ser desfeita no ambiente ativo.")
                .setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w)->executar("EXCLUSAO")).show());
    }
    private void executar(String acao) {
        if(ocupado)return;
        String atual=senha==null?"":senha.getText().toString();
        if(!acao.equals("LISTAR")&&!acao.equals("ACEITAR")&&atual.isEmpty()){resultado.setText("Informe sua senha atual.");return;}
        ocupado=true;
        Tarefas.executar(()->{
            try {
            Api api=new Api(this); JSONObject dados=new JSONObject(); dados.put("senha",atual);
            if(acao.equals("LISTAR"))return api.get("/api/v1/usuarios/me/privacidade");
            if(acao.equals("ACEITAR")){
                dados=new JSONObject();dados.put("aceiteTermos",true);dados.put("versaoTermos","2026-11-25");dados.put("versaoPrivacidade","2026-11-25");dados.put("origem","ANDROID");
                api.post("/api/v1/usuarios/me/aceites",dados);return "Aceite registrado.";
            }
            if(acao.equals("EXPORTAR"))return api.post("/api/v1/usuarios/me/privacidade/exportacao",dados);
            dados.put("tipo",acao);
            String resposta=api.post("/api/v1/usuarios/me/privacidade/solicitacoes",dados);
            if(acao.equals("EXCLUSAO")&&new JSONObject(resposta).optString("status").equals("CONCLUIDA")){
                new FavoritoRepository(this).limparParaExclusao(); Sessao.limpar(this);
            }
            return resposta;
            } catch(org.json.JSONException e){throw new ApiException(500,null,e);}
        },new Tarefas.Callback<String>(){
            public void sucesso(String valor){ocupado=false;if(senha!=null)senha.setText("");
                if(acao.equals("EXPORTAR")){exportacao=valor;salvar.launch("dados-talisma.json");}
                else if(acao.equals("EXCLUSAO")&&!Sessao.logado(PrivacidadeActivity.this)){
                    corpo.removeAllViews();texto("Conta excluída. Guarde o protocolo:",20);texto(valor,16);botao("Voltar",()->{startActivity(new android.content.Intent(PrivacidadeActivity.this,HomeActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();});
                }else resultado.setText(formatar(valor));
            }
            public void erro(ApiException e){ocupado=false;resultado.setText(e.mensagemParaUsuario(PrivacidadeActivity.this));}
        });
    }
    private static String formatar(String valor){try{return new JSONObject(valor).toString(2);}catch(Exception e){return valor;}}
    private void documento(String nome){
        try(var in=getAssets().open("legal/"+nome+".txt")){
            var out=new java.io.ByteArrayOutputStream(); byte[] buffer=new byte[4096];int l;while((l=in.read(buffer))!=-1)out.write(buffer,0,l);
            TextView texto=new TextView(this);texto.setPadding(24,16,24,16);texto.setText(new String(out.toByteArray(),StandardCharsets.UTF_8));texto.setTextIsSelectable(true);
            ScrollView scroll=new ScrollView(this);scroll.addView(texto);
            new MaterialAlertDialogBuilder(this).setView(scroll).setPositiveButton("Fechar",null).show();
        }catch(Exception e){resultado.setText("Documento indisponível.");}
    }
    private TextView texto(String valor,int tamanho){TextView v=new TextView(this);v.setText(valor);v.setTextSize(tamanho);v.setPadding(0,12,0,12);corpo.addView(v);return v;}
    private void botao(String titulo,Runnable acao){Button b=new Button(this);b.setText(titulo);b.setOnClickListener(v->acao.run());corpo.addView(b);}
    private Tarefas.Callback<String> retorno(){return new Tarefas.Callback<String>(){public void sucesso(String v){resultado.setText(v);}public void erro(ApiException e){resultado.setText(e.mensagemParaUsuario(PrivacidadeActivity.this));}};}
}

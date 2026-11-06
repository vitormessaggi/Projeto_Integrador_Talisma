package br.com.talisma.imoveis;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import br.com.talisma.imoveis.dados.Visita;
import br.com.talisma.imoveis.util.CarregadorImagem;
import br.com.talisma.imoveis.util.Formatos;

/**
 * Adapter de "Meus contatos com corretores" (D37, 6.5): foto, título do imóvel, "12/11/2026 · Tarde",
 * chip de status colorido e botão "Cancelar" quando a visita ainda pode ser cancelada.
 */
public class ContatoAdapter extends RecyclerView.Adapter<ContatoAdapter.Holder> {

    public interface Ouvinte {
        void aoTocar(Visita visita);

        void aoCancelar(Visita visita, int posicao);
    }

    private final Ouvinte ouvinte;
    private final List<Visita> itens = new ArrayList<>();

    public ContatoAdapter(Ouvinte ouvinte) {
        this.ouvinte = ouvinte;
        setHasStableIds(true);
    }

    public void substituir(List<Visita> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    public void atualizar(int posicao, Visita visita) {
        itens.set(posicao, visita);
        notifyItemChanged(posicao);
    }

    @Override
    public long getItemId(int position) {
        return itens.get(position).id;
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contato, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Visita visita = itens.get(position);
        Context c = h.itemView.getContext();
        h.txtTitulo.setText(visita.imovelTitulo);
        h.txtDataPeriodo.setText(c.getString(R.string.contatos_data_periodo, Formatos.data(visita.data),
                periodo(c, visita.periodo)));
        h.chipStatus.setText(status(c, visita.status));
        h.chipStatus.setChipBackgroundColor(ContextCompat.getColorStateList(c, corStatus(visita.status)));
        h.btnCancelar.setVisibility(visita.podeCancelar() ? View.VISIBLE : View.GONE);
        CarregadorImagem.carregar(h.imgCapa, visita.imovelFoto);
        h.itemView.setOnClickListener(v -> ouvinte.aoTocar(visita));
        h.btnCancelar.setOnClickListener(v -> ouvinte.aoCancelar(visita, h.getBindingAdapterPosition()));
    }

    public static String periodo(Context c, String periodo) {
        if (Visita.MANHA.equals(periodo)) {
            return c.getString(R.string.visita_manha);
        }
        if (Visita.NOITE.equals(periodo)) {
            return c.getString(R.string.visita_noite);
        }
        return c.getString(R.string.visita_tarde);
    }

    private static String status(Context c, String status) {
        if (Visita.CONFIRMADA.equals(status)) {
            return c.getString(R.string.contatos_status_confirmada);
        }
        if (Visita.CONCLUIDA.equals(status)) {
            return c.getString(R.string.contatos_status_concluida);
        }
        if (Visita.CANCELADA.equals(status)) {
            return c.getString(R.string.contatos_status_cancelada);
        }
        return c.getString(R.string.contatos_status_pendente);
    }

    private static int corStatus(String status) {
        if (Visita.CONFIRMADA.equals(status)) {
            return R.color.sucesso;
        }
        if (Visita.CONCLUIDA.equals(status)) {
            return R.color.texto_secundario;
        }
        if (Visita.CANCELADA.equals(status)) {
            return R.color.erro;
        }
        return R.color.alerta;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView imgCapa;
        final TextView txtTitulo;
        final TextView txtDataPeriodo;
        final Chip chipStatus;
        final View btnCancelar;

        Holder(View v) {
            super(v);
            imgCapa = v.findViewById(R.id.imgCapa);
            txtTitulo = v.findViewById(R.id.txtTitulo);
            txtDataPeriodo = v.findViewById(R.id.txtDataPeriodo);
            chipStatus = v.findViewById(R.id.chipStatus);
            btnCancelar = v.findViewById(R.id.btnCancelar);
        }
    }
}

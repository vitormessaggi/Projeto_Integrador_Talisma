package br.com.talisma.imoveis;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.com.talisma.imoveis.dados.FotoEnvio;
import br.com.talisma.imoveis.util.CarregadorImagem;

/**
 * Novas fotos do corretor (FotosImovel, 6.5, 2.5.2): grade com lixeira por foto e estado
 * aguardando, enviando com ProgressBar, enviada ✓ ou falhou ↻ (toque reenvia).
 * A lista pertence à Activity (é ela que a salva na rotação — D20).
 */
public class FotoNovaAdapter extends RecyclerView.Adapter<FotoNovaAdapter.Holder> {

    public interface Ouvinte {
        void aoRemover(int posicao);

        void aoReenviar(int posicao);
    }

    private final List<FotoEnvio> fotos;
    private final Ouvinte ouvinte;

    public FotoNovaAdapter(List<FotoEnvio> fotos, Ouvinte ouvinte) {
        this.fotos = fotos;
        this.ouvinte = ouvinte;
    }

    @Override
    public int getItemCount() {
        return fotos.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_foto_nova, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        FotoEnvio foto = fotos.get(position);
        Context context = holder.itemView.getContext();
        CarregadorImagem.carregar(holder.imgFoto, foto.uri.toString());

        boolean enviando = FotoEnvio.ENVIANDO.equals(foto.estado);
        boolean enviada = FotoEnvio.ENVIADA.equals(foto.estado);
        boolean falhou = FotoEnvio.FALHOU.equals(foto.estado);

        holder.vwVeu.setVisibility(enviando || enviada || falhou ? View.VISIBLE : View.GONE);
        holder.prgEnvio.setVisibility(enviando ? View.VISIBLE : View.GONE);
        holder.prgEnvio.setProgress(foto.progresso);
        holder.imgEstado.setVisibility(enviada || falhou ? View.VISIBLE : View.GONE);
        holder.imgEstado.setImageResource(falhou ? R.drawable.ic_reenviar : R.drawable.ic_check);
        holder.imgEstado.setContentDescription(context.getString(falhou ? R.string.desc_foto_falhou : R.string.desc_foto_enviada));
        // Lixeira só enquanto a foto ainda não foi (ou está sendo) enviada
        holder.btnRemover.setVisibility(enviando || enviada ? View.GONE : View.VISIBLE);

        if (enviando) {
            holder.imgFoto.setContentDescription(context.getString(R.string.desc_foto_enviando, foto.progresso));
        } else if (falhou) {
            holder.imgFoto.setContentDescription(context.getString(R.string.desc_foto_falhou));
        } else {
            holder.imgFoto.setContentDescription(context.getString(
                    enviada ? R.string.desc_foto_enviada : R.string.desc_foto_nova));
        }

        holder.btnRemover.setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao != RecyclerView.NO_POSITION) {
                ouvinte.aoRemover(posicao);
            }
        });
        holder.imgFoto.setOnClickListener(falhou ? v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao != RecyclerView.NO_POSITION) {
                ouvinte.aoReenviar(posicao);
            }
        } : null);
        holder.imgFoto.setClickable(falhou);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView imgFoto;
        final View vwVeu;
        final ProgressBar prgEnvio;
        final ImageView imgEstado;
        final ImageButton btnRemover;

        Holder(View view) {
            super(view);
            imgFoto = view.findViewById(R.id.imgFoto);
            vwVeu = view.findViewById(R.id.vwVeu);
            prgEnvio = view.findViewById(R.id.prgEnvio);
            imgEstado = view.findViewById(R.id.imgEstado);
            btnRemover = view.findViewById(R.id.btnRemover);
        }
    }
}

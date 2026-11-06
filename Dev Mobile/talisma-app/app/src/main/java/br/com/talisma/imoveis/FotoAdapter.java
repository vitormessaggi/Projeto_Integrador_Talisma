package br.com.talisma.imoveis;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.com.talisma.imoveis.util.CarregadorImagem;

/**
 * Fotos de um imóvel (6.2): {@code MINIATURA} (72dp, Detalhe) ou {@code GALERIA} (tela cheia).
 * O toque devolve a posição para abrir a Galeria naquela foto.
 */
public class FotoAdapter extends RecyclerView.Adapter<FotoAdapter.Holder> {

    public enum Modo { MINIATURA, GALERIA }

    public interface Ouvinte {
        void aoTocar(int posicao);
    }

    private final Modo modo;
    private final Ouvinte ouvinte;
    private final List<String> fotos = new ArrayList<>();

    public FotoAdapter(Modo modo, Ouvinte ouvinte) {
        this.modo = modo;
        this.ouvinte = ouvinte;
    }

    public void substituir(List<String> novas) {
        fotos.clear();
        fotos.addAll(novas);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return fotos.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = modo == Modo.GALERIA ? R.layout.item_foto_galeria : R.layout.item_miniatura;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ImageView img = holder.img;
        CarregadorImagem.carregar(img, fotos.get(position));
        img.setContentDescription(img.getContext().getString(R.string.desc_miniatura, position + 1, fotos.size()));
        if (ouvinte != null) {
            img.setOnClickListener(v -> {
                int posicao = holder.getBindingAdapterPosition();
                if (posicao != RecyclerView.NO_POSITION) {
                    ouvinte.aoTocar(posicao);
                }
            });
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView img;

        Holder(View view) {
            super(view);
            img = (ImageView) view;
        }
    }
}

package br.com.talisma.imoveis;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.com.talisma.imoveis.dados.Imovel;
import br.com.talisma.imoveis.util.CarregadorImagem;

/**
 * Imóveis do corretor (SelecionarImovel, D33, 6.5): CartaoImovel com a contagem "4 fotos"
 * ({@code imovel.fotos.size()}); o toque abre FotosImovel.
 */
public class ImovelCorretorAdapter extends RecyclerView.Adapter<ImovelCorretorAdapter.Holder> {

    public interface Ouvinte {
        void aoTocar(Imovel imovel);
    }

    private final Ouvinte ouvinte;
    private final List<Imovel> itens = new ArrayList<>();

    public ImovelCorretorAdapter(Ouvinte ouvinte) {
        this.ouvinte = ouvinte;
        setHasStableIds(true);
    }

    public void substituir(List<Imovel> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_imovel_corretor, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Imovel imovel = itens.get(position);
        Context context = holder.itemView.getContext();
        holder.txtTitulo.setText(imovel.titulo);
        holder.txtLocalizacao.setText(ImovelAdapter.localizacao(context, imovel));
        int fotos = imovel.fotos.size();
        holder.txtContagemFotos.setText(context.getResources().getQuantityString(
                R.plurals.corretor_contagem_fotos, fotos, fotos));
        CarregadorImagem.carregar(holder.imgCapa, imovel.capa());
        holder.itemView.setOnClickListener(v -> ouvinte.aoTocar(imovel));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView imgCapa;
        final TextView txtTitulo;
        final TextView txtLocalizacao;
        final TextView txtContagemFotos;

        Holder(View view) {
            super(view);
            imgCapa = view.findViewById(R.id.imgCapa);
            txtTitulo = view.findViewById(R.id.txtTitulo);
            txtLocalizacao = view.findViewById(R.id.txtLocalizacao);
            txtContagemFotos = view.findViewById(R.id.txtContagemFotos);
        }
    }
}

package br.com.talisma.imoveis;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.talisma.imoveis.dados.Imovel;
import br.com.talisma.imoveis.util.CarregadorImagem;
import br.com.talisma.imoveis.util.Formatos;

/**
 * Adapter dos imóveis (6.2): {@code LISTA} usa o CartaoImovel horizontal (Imóveis, Favoritos) e
 * {@code DESTAQUE} o CartaoDestaque vertical do carrossel da Home. Mostra o coração conforme os
 * ids favoritos e "a X km" quando o imóvel tem {@link Imovel#distanciaKm} (D15).
 */
public class ImovelAdapter extends RecyclerView.Adapter<ImovelAdapter.Holder> {

    public enum Modo { LISTA, DESTAQUE }

    /** Toques no cartão e no coração. */
    public interface Ouvinte {
        void aoTocar(Imovel imovel);

        void aoFavoritar(Imovel imovel, int posicao);
    }

    private final Modo modo;
    private final Ouvinte ouvinte;
    private final List<Imovel> itens = new ArrayList<>();
    private final Set<Long> favoritos = new HashSet<>();

    public ImovelAdapter(Modo modo, Ouvinte ouvinte) {
        this.modo = modo;
        this.ouvinte = ouvinte;
        setHasStableIds(true);
    }

    public List<Imovel> getItens() {
        return itens;
    }

    public void substituir(List<Imovel> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    public void acrescentar(List<Imovel> novos) {
        int inicio = itens.size();
        itens.addAll(novos);
        notifyItemRangeInserted(inicio, novos.size());
    }

    public void removerEm(int posicao) {
        itens.remove(posicao);
        notifyItemRemoved(posicao);
    }

    public void inserirEm(int posicao, Imovel imovel) {
        itens.add(Math.min(posicao, itens.size()), imovel);
        notifyItemInserted(Math.min(posicao, itens.size() - 1));
    }

    /** Troca o conjunto de favoritos e redesenha os corações. */
    public void setFavoritos(Set<Long> ids) {
        favoritos.clear();
        favoritos.addAll(ids);
        notifyDataSetChanged();
    }

    public void marcarFavorito(long imovelId, boolean favorito) {
        if (favorito) {
            favoritos.add(imovelId);
        } else {
            favoritos.remove(imovelId);
        }
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).id == imovelId) {
                notifyItemChanged(i);
            }
        }
    }

    public boolean isFavorito(long imovelId) {
        return favoritos.contains(imovelId);
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
        int layout = modo == Modo.DESTAQUE ? R.layout.item_destaque : R.layout.item_imovel;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Imovel imovel = itens.get(position);
        Context context = holder.itemView.getContext();
        holder.txtTitulo.setText(imovel.titulo);
        holder.txtLocalizacao.setText(localizacao(context, imovel));
        holder.txtPreco.setText(Formatos.preco(context, imovel));
        CarregadorImagem.carregar(holder.imgCapa, imovel.capa());

        boolean favorito = favoritos.contains(imovel.id);
        holder.btnFavorito.setImageResource(favorito ? R.drawable.ic_favorito_cheio : R.drawable.ic_favorito);
        holder.btnFavorito.setContentDescription(
                context.getString(favorito ? R.string.desc_remover_favorito : R.string.desc_favoritar));

        if (holder.txtDistancia != null) {
            if (imovel.distanciaKm != null) {
                holder.txtDistancia.setText(Formatos.distancia(context, imovel.distanciaKm));
                holder.txtDistancia.setVisibility(View.VISIBLE);
            } else {
                holder.txtDistancia.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> ouvinte.aoTocar(imovel));
        holder.btnFavorito.setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao != RecyclerView.NO_POSITION) {
                ouvinte.aoFavoritar(itens.get(posicao), posicao);
            }
        });
    }

    /** "Bairro, Cidade" ou só a cidade. */
    public static String localizacao(Context context, Imovel imovel) {
        if (imovel.bairro == null || imovel.bairro.isEmpty()) {
            return imovel.cidade != null ? imovel.cidade : "";
        }
        if (imovel.cidade == null || imovel.cidade.isEmpty()) {
            return imovel.bairro;
        }
        return context.getString(R.string.formato_localizacao, imovel.bairro, imovel.cidade);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView imgCapa;
        final TextView txtTitulo;
        final TextView txtLocalizacao;
        final TextView txtPreco;
        final TextView txtDistancia;
        final ImageButton btnFavorito;

        Holder(View view) {
            super(view);
            imgCapa = view.findViewById(R.id.imgCapa);
            txtTitulo = view.findViewById(R.id.txtTitulo);
            txtLocalizacao = view.findViewById(R.id.txtLocalizacao);
            txtPreco = view.findViewById(R.id.txtPreco);
            txtDistancia = view.findViewById(R.id.txtDistancia);
            btnFavorito = view.findViewById(R.id.btnFavorito);
        }
    }
}

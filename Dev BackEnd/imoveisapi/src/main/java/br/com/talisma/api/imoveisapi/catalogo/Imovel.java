package br.com.talisma.api.imoveisapi.catalogo;
import jakarta.persistence.*;
@Entity
@Table(name="imovel")
public class Imovel {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 String titulo;
 String descricao;
 String tipo;
 String status;
 String cidade;
 String bairro;
 String uf;
 Integer quartos;
 Integer banheiros;
 Integer vagas;
 java.math.BigDecimal areaTotal;
 java.math.BigDecimal latitude;
 java.math.BigDecimal longitude;
 Boolean destaque;
 protected Imovel() {}
}

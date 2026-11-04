package br.com.talisma.api.imoveisapi.catalogo;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="oferta")
public class Oferta {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(name="imovel_id", nullable=false) Long imovelId;
 @Column(nullable=false) String finalidade;
 @Column(nullable=false, precision=15, scale=2) BigDecimal preco;
 protected Oferta() {}
}

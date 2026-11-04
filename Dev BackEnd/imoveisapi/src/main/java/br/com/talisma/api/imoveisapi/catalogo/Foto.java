package br.com.talisma.api.imoveisapi.catalogo;

import jakarta.persistence.*;

@Entity
@Table(name = "foto")
public class Foto {
    @Id
    Long id;

    @Column(name = "imovel_id")
    Long imovelId;

    int ordem;

    protected Foto() {}
}

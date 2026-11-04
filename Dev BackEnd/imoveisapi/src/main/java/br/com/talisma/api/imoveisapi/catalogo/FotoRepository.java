package br.com.talisma.api.imoveisapi.catalogo;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface FotoRepository extends JpaRepository<Foto, Long> {
    List<Foto> findByImovelIdInOrderByOrdemAscIdAsc(Collection<Long> imovelIds);
}

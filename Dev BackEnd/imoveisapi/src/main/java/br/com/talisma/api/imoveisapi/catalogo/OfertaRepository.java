package br.com.talisma.api.imoveisapi.catalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface OfertaRepository extends JpaRepository<Oferta,Long> {
 List<Oferta> findByImovelIdInOrderByIdAsc(Collection<Long> ids);
}

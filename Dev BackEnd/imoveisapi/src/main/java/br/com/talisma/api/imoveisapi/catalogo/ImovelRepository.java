package br.com.talisma.api.imoveisapi.catalogo;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
interface ImovelRepository extends JpaRepository<Imovel,Long> {
 @Query("""
 select i from Imovel i where i.status = 'PUBLICADO'
 and (:cidade is null or i.cidade = :cidade)
 and (:tipo is null or i.tipo = :tipo)
 and (:quartos is null or i.quartos >= :quartos)
 and (:destaque is null or i.destaque = :destaque)
 and exists (select o.id from Oferta o where o.imovelId = i.id
 and (:finalidade is null or o.finalidade = :finalidade)
 and (:min is null or o.preco >= :min)
 and (:max is null or o.preco <= :max))
 """)
 Page<Imovel> buscar(@Param("cidade") String cidade,@Param("tipo") String tipo,
 @Param("quartos") Integer quartos,@Param("destaque") Boolean destaque,
 @Param("finalidade") String finalidade,@Param("min") BigDecimal min,
 @Param("max") BigDecimal max, Pageable pageable);
 java.util.Optional<Imovel> findByIdAndStatus(Long id, String status);

 @Query("select distinct i.tipo from Imovel i where i.status = 'PUBLICADO' order by i.tipo")
 java.util.List<String> tiposPublicados();

 @Query("select distinct i.cidade from Imovel i where i.status = 'PUBLICADO' order by i.cidade")
 java.util.List<String> cidadesPublicadas(); @Query(value = "SELECT i.* FROM imovel i JOIN favorito f ON f.imovel_id=i.id WHERE f.usuario_id=:usuarioId AND i.status='PUBLICADO'",
        countQuery = "SELECT COUNT(*) FROM imovel i JOIN favorito f ON f.imovel_id=i.id WHERE f.usuario_id=:usuarioId AND i.status='PUBLICADO'",
        nativeQuery = true)
 Page<Imovel> favoritos(@Param("usuarioId") long usuarioId, Pageable pageable);
}

ALTER TABLE imovel ADD CONSTRAINT ck_imovel_coordenadas
CHECK ((latitude IS NULL AND longitude IS NULL) OR
       (latitude IS NOT NULL AND longitude IS NOT NULL AND
        latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180));

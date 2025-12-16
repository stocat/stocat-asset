package com.stocat.asset.mysql.domain.asset.repository;

import com.stocat.asset.mysql.domain.asset.domain.AssetsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetsRepository extends JpaRepository<AssetsEntity, Long> {
}

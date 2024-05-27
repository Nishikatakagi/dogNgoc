package com.nishikatakagi.store.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nishikatakagi.store.models.ProductHistory;

public interface ProductHistoryRepository extends JpaRepository<ProductHistory,Integer>{

}

package com.nishikatakagi.store.Repository;

import com.nishikatakagi.store.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product,Integer> {
}

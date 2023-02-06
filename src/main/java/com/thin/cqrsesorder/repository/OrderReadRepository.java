package com.thin.cqrsesorder.repository;

import com.thin.cqrsesorder.bean.view.OrderView;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderReadRepository extends ElasticsearchRepository<OrderView, String> {

}

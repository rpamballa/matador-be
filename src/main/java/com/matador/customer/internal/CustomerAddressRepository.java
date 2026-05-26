package com.matador.customer.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findByCustomerIdOrderByCreatedAtAsc(UUID customerId);

    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);

    @Modifying
    @Query("update CustomerAddress a set a.isDefault = false where a.customerId = :customerId")
    void clearDefaultFor(@Param("customerId") UUID customerId);
}

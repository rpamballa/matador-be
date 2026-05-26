package com.matador.customer.internal;

import com.matador.customer.Customer;
import com.matador.customer.CustomerStatus;
import com.matador.customer.VerificationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    @Query(
        """
        select c from Customer c
        where (:email is null or lower(c.email) = lower(:email))
          and (:phone is null or c.phone = :phone)
          and (:verificationStatus is null or c.verificationStatus = :verificationStatus)
          and (:status is null or c.status = :status)
        """)
    Page<Customer> search(
        @Param("email") String email,
        @Param("phone") String phone,
        @Param("verificationStatus") VerificationStatus verificationStatus,
        @Param("status") CustomerStatus status,
        Pageable pageable);

    List<Customer> findByVerificationStatusAndLicenseExpiresOnBefore(
        VerificationStatus status, LocalDate date);
}

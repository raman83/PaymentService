// Repo
package com.payment.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.payment.model.Biller;
import com.payment.model.ExternalCounterparty;

import java.util.*;

public interface CounterpartyRepository extends JpaRepository<ExternalCounterparty, UUID> {
    List<ExternalCounterparty> findByCustomerId(String customerId);
	boolean existsByCustomerIdAndAccountNumber(String customerId, String accountNumber);

}

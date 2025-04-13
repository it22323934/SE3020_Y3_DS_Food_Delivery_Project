package DriverOrderService.repository;

import DriverOrderService.models.DriverOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverOrderRepository extends JpaRepository<DriverOrder, String> {

    List<DriverOrder> findByDriverIdAndIsOrderComplete(String driverId, Boolean isOrderComplete);
}


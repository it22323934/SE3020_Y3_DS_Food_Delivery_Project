package Delivery_driverService.repository;

import Delivery_driverService.models.DeliveryDriver;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DeliveryDriverRepository extends MongoRepository<DeliveryDriver,String> {

    List<DeliveryDriver> findByWorkingCity(String workingCity);
}

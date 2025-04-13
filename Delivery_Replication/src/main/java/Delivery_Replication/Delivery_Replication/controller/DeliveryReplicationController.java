package Delivery_Replication.Delivery_Replication.controller;

import Delivery_Replication.Delivery_Replication.dto.DeliveryReplicationRequest;
import Delivery_Replication.Delivery_Replication.dto.DeliveryReplicationResponse;
import Delivery_Replication.Delivery_Replication.models.DeliveryReplication;
import Delivery_Replication.Delivery_Replication.service.DeliveryReplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveryReplication")
public class DeliveryReplicationController {

    DeliveryReplication delivery = new DeliveryReplication();
    private final DeliveryReplicationService deliveryReplicationService;

    public DeliveryReplicationController(DeliveryReplicationService deliveryReplicationService) {
        this.deliveryReplicationService = deliveryReplicationService;
    }

    @GetMapping
    public List<DeliveryReplicationResponse> getAllDeliveries() {
        return deliveryReplicationService.getAllDeliveryReplicationResponses();
    }



    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryReplicationResponse createDelivery(@RequestBody DeliveryReplicationRequest request) {
        return deliveryReplicationService.createDeliveryReplication(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDelivery(@PathVariable String id) {
        deliveryReplicationService.deleteDeliveryReplication(id);
    }

    // Updated method to return a list of responses based on isAssignDriver
    @GetMapping("/by-assign-driver")
    public List<DeliveryReplicationResponse> getDeliveriesByAssignDriver() {
        return deliveryReplicationService.getDeliveriesByAssignDriver(false);
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<DeliveryReplicationResponse> updateDelivery(
            @PathVariable String orderId,
            @RequestBody DeliveryReplicationRequest request) {
        DeliveryReplicationResponse updated = deliveryReplicationService.updateDeliveryReplication(orderId,delivery );
        return ResponseEntity.ok(updated);
    }


}

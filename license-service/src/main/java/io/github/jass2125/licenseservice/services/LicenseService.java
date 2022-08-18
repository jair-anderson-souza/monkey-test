package io.github.jass2125.licenseservice.services;

import io.github.jass2125.licenseservice.exceptions.LicenseNotFoundException;
import io.github.jass2125.licenseservice.integration.events.EventPublisher;
import io.github.jass2125.licenseservice.integration.feign.OrderFeignClient;
import io.github.jass2125.licenseservice.model.License;
import io.github.jass2125.licenseservice.repositories.LicenseRepository;
import io.github.jass2125.licenseservice.integration.events.LicenseEvent;
import io.github.jass2125.licenseservice.controller.LicenseRequest;
import io.github.jass2125.licenseservice.controller.LicenseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class LicenseService {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private OrderFeignClient orderFeignRepository;

    @Cacheable(cacheNames = "License", key = "#root.method.name")
    public List<LicenseResponse> findAll() {
        final List<License> list = this.licenseRepository.findAll();
        return list.stream().map((license) -> new LicenseResponse(license.getId(), license.getName())).collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "License", key = "#id")
    public LicenseResponse findById(final Long id) {
        final var license = this.licenseRepository.findById(id).orElseThrow(() -> new LicenseNotFoundException("License not found") );
        return new LicenseResponse(license.getId(), license.getName());

    }

    @Transactional(propagation = Propagation.REQUIRED)
    @CacheEvict(cacheNames = "License", allEntries = true)
    public LicenseResponse save(final LicenseRequest licenseRequest) {
        var lic = new License(licenseRequest.getProductName(), licenseRequest.getName(), licenseRequest.getContactName(), licenseRequest.getContactEmail(), licenseRequest.getContactPhone());
        final var license = this.licenseRepository.save(lic);
        final var licenseEvent = new LicenseEvent(license);
        this.eventPublisher.publish(licenseEvent);
        return new LicenseResponse(license.getId(), license.getName());

    }
}


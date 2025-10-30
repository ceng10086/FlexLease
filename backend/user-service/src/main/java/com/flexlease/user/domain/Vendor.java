package com.flexlease.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor", schema = "users")
public class Vendor {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 50)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "address", length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VendorStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Vendor() {
        // JPA
    }

    private Vendor(UUID id,
                   UUID ownerUserId,
                   String companyName,
                   String contactName,
                   String contactPhone,
                   String contactEmail,
                   String province,
                   String city,
                   String address,
                   VendorStatus status) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.companyName = companyName;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.province = province;
        this.city = city;
        this.address = address;
        this.status = status;
    }

    public static Vendor create(UUID ownerUserId,
                                String companyName,
                                String contactName,
                                String contactPhone,
                                String contactEmail,
                                String province,
                                String city,
                                String address) {
        return new Vendor(UUID.randomUUID(), ownerUserId, companyName, contactName, contactPhone, contactEmail, province, city, address, VendorStatus.ACTIVE);
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public VendorStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateContactInfo(String contactName,
                                  String contactPhone,
                                  String contactEmail,
                                  String province,
                                  String city,
                                  String address) {
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.province = province;
        this.city = city;
        this.address = address;
    }

    public void updateStatus(VendorStatus status) {
        this.status = status;
    }

    public void updateCompanyName(String companyName) {
        this.companyName = companyName;
    }
}

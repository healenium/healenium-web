package com.epam.healenium.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class TenantSettingsDto {

    private UUID tenantId;
    private Boolean healEnabled;
    private Integer recoveryTries;
    private Double scoreCap;
    private String healedType;
}

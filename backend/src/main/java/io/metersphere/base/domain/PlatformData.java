package io.metersphere.base.domain;

import java.io.Serializable;
import lombok.Data;

@Data
public class PlatformData implements Serializable {
    private String id;

    private String recordId;

    private String platform;

    private String platformId;

    private String platformData;

    private static final long serialVersionUID = 1L;
}
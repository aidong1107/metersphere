package io.metersphere.track.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Describe
 *
 * @author maguangming
 * @since 2022/1/13 17:41
 */

@Getter
@Setter
public class IssueAttachmentDTO {

    private String id;
    private String name;
    private Long size;
    private String type;
    private Long updateTime;
    private String url;
}

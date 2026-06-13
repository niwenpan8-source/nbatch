package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class StopRunNodeParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private String runWorkId;

    private List<String> runNodeIdList;

    private List<String> nodeLogIdList;
}

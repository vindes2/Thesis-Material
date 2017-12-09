package com.unina.checkfiles;

import com.polarion.platform.jobs.IJobUnit;

public interface ICheckFile extends IJobUnit {
	static final String JOB_NAME = "checkfiles";
	void setPath(String path); //il formato deve essere questo	

}

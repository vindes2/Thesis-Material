package com.unina.updatetr;
import com.polarion.platform.jobs.IJobUnit;

public interface IUpdateTr extends IJobUnit {

	static final String JOB_NAME = "updatetr";				
	void setUser(String user); //il formato deve essere questo	
	void setPath(String path); //il formato deve essere questo	
}

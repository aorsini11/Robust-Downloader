package com.example.alex.robustdownload_orsini;

import java.util.Date;

/**
 * Created by Alex on 9/20/15.
 */
public class ConnectionInfo {

    public String connectionname;
    public boolean rutgersWifi;
    public Date connectionStart;
    public Date connectionEnd;

    public ConnectionInfo(String connectionname,Date connectionStart){
        this.connectionname = connectionname;
        this.connectionStart = connectionStart;
    }
}

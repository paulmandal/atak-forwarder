package com.paulmandal.atak.forwarder.comm.protobuf;

public class CustomBytesExtFields {
    public String how;
    public String geoPointSrc;
    public String altSrc;
    public String role;
    public Integer battery;
    public Boolean readiness;
    public Boolean labelsOn;
    public Integer heightUnit;
    public Integer heightValue;

    public CustomBytesExtFields(String how, String geoPointSrc, String altSrc, String role, Integer battery, Boolean readiness, Boolean labelsOn, Integer heightUnit, Integer heightValue) {
        this.how = how;
        this.geoPointSrc = geoPointSrc;
        this.altSrc = altSrc;
        this.role = role;
        this.battery = battery;
        this.readiness = readiness;
        this.labelsOn = labelsOn;
        this.heightUnit = heightUnit;
        this.heightValue = heightValue;
    }
}
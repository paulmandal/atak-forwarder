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
    public Boolean ceHumanInput;
    public Boolean tog;
    public String routePlanningMethod;
    public String routeMethod;
    public String routeType;
    public String routeRouteType;
    public String routeOrder;
    public Integer routeStroke;

    public CustomBytesExtFields(String how,
                                String geoPointSrc,
                                String altSrc,
                                String role,
                                Integer battery,
                                Boolean readiness,
                                Boolean labelsOn,
                                Integer heightUnit,
                                Boolean ceHumanInput,
                                Boolean tog,
                                String routePlanningMethod,
                                String routeMethod,
                                String routeType,
                                String routeRouteType,
                                String routeOrder,
                                Integer routeStroke) {
        this.how = how;
        this.geoPointSrc = geoPointSrc;
        this.altSrc = altSrc;
        this.role = role;
        this.battery = battery;
        this.readiness = readiness;
        this.labelsOn = labelsOn;
        this.heightUnit = heightUnit;
        this.ceHumanInput = ceHumanInput;
        this.tog = tog;
        this.routePlanningMethod = routePlanningMethod;
        this.routeMethod = routeMethod;
        this.routeType = routeType;
        this.routeRouteType = routeRouteType;
        this.routeOrder = routeOrder;
        this.routeStroke = routeStroke;
    }
}
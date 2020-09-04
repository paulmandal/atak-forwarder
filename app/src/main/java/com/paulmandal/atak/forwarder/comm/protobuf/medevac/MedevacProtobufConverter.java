package com.paulmandal.atak.forwarder.comm.protobuf.medevac;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.paulmandal.atak.forwarder.comm.protobuf.UnknownDetailFieldException;
import com.paulmandal.atak.forwarder.protobufs.ProtobufMedevac;

public class MedevacProtobufConverter {
    private static final String KEY_MEDEVAC = "_medevac_";

    private static final String KEY_MISTS_MAP = "zMistsMap";

    private static final String KEY_TITLE = "title";
    private static final String KEY_ZONE_PROT_SELECTION = "zone_prot_selection";
    private static final String KEY_FREQUENCY = "freq";
    private static final String KEY_URGENT = "urgent";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_ROUTINE = "routine";
    private static final String KEY_HOIST = "hoist";
    private static final String KEY_VENTILATOR = "ventilator";
    private static final String KEY_EQUIPMENT_OTHER = "equipment_other";
    private static final String KEY_EXTRACTION_EQUIPMENT = "extraction_equipment";
    private static final String KEY_EQUIPMENT_NONE = "equipment_none";
    private static final String KEY_EQUIPMENT_DETAIL = "equipment_detail";
    private static final String KEY_LITTER = "litter";
    private static final String KEY_AMBULATORY = "ambulatory";
    private static final String KEY_SECURITY = "security";
    private static final String KEY_HLZ_MARKING = "hlz_marking";
    private static final String KEY_NONUS_CIVILIAN = "nonus_civilian";
    private static final String KEY_US_CIVILIAN = "us_civilian";
    private static final String KEY_CHILD = "child";
    private static final String KEY_EPW = "epw";
    private static final String KEY_US_MILITARY = "us_military";
    private static final String KEY_NONUS_MILITARY = "nonus_military";
    private static final String KEY_TERRAIN_SLOPE = "terrain_slope";
    private static final String KEY_TERRAIN_ROUGH = "terrain_rough";
    private static final String KEY_TERRAIN_NONE = "terrain_none";
    private static final String KEY_TERRAIN_OTHER = "terrain_other";
    private static final String KEY_TERRAIN_LOOSE = "terrain_loose";
    private static final String KEY_TERRAIN_OTHER_DETAIL = "terrain_other_detail";
    private static final String KEY_TERRAIN_SLOPE_DIR = "terrain_slope_dir";
    private static final String KEY_MEDLINE_REMARKS = "medline_remarks";
    private static final String KEY_MARKED_BY = "marked_by";
    private static final String KEY_OBSTACLES = "obstacles";
    private static final String KEY_WINDS_ARE_FROM = "winds_are_from";
    private static final String KEY_FRIENDLIES = "friendlies";
    private static final String KEY_ENEMY = "enemy";
    private static final String KEY_HLZ_REMARKS = "hlz_remarks";
    private static final String KEY_HLZ_OTHER = "hlz_other";
    private static final String KEY_CASEVAC = "casevac";

    private MistsMapProtobufConverter mMistsMapProtobufConverter;

    public MedevacProtobufConverter(MistsMapProtobufConverter mistsMapProtobufConverter) {
        mMistsMapProtobufConverter = mistsMapProtobufConverter;
    }


    public ProtobufMedevac.Medevac toMedevac(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufMedevac.Medevac.Builder builder = ProtobufMedevac.Medevac.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_TITLE:
                    builder.setTitle(attribute.getValue());
                    break;
                case KEY_ZONE_PROT_SELECTION:
                    builder.setZoneProtSelection(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_FREQUENCY:
                    builder.setFreq(attribute.getValue());
                    break;
                case KEY_URGENT:
                    builder.setUrgent(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_PRIORITY:
                    builder.setPriority(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_ROUTINE:
                    builder.setRoutine(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_HOIST:
                    builder.setHoist(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_VENTILATOR:
                    builder.setVentilator(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_EQUIPMENT_OTHER:
                    builder.setEquipmentOther(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_EXTRACTION_EQUIPMENT:
                    builder.setExtractionEquipment(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_EQUIPMENT_NONE:
                    builder.setEquipmentNone(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_EQUIPMENT_DETAIL:
                    builder.setEquipmentDetail(attribute.getValue());
                    break;
                case KEY_LITTER:
                    builder.setLitter(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_AMBULATORY:
                    builder.setAmbulatory(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_SECURITY:
                    builder.setSecurity(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_HLZ_MARKING:
                    builder.setHlzMarking(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_NONUS_CIVILIAN:
                    builder.setNonusCivilian(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_US_CIVILIAN:
                    builder.setUsCivilian(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_CHILD:
                    builder.setChild(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_EPW:
                    builder.setEpw(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_US_MILITARY:
                    builder.setUsMilitary(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_NONUS_MILITARY:
                    builder.setNonusMilitary(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_TERRAIN_SLOPE:
                    builder.setTerrainSlope(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_TERRAIN_ROUGH:
                    builder.setTerrainRough(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_TERRAIN_NONE:
                    builder.setTerrainNone(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_TERRAIN_OTHER:
                    builder.setTerrainOther(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_TERRAIN_LOOSE:
                    builder.setTerrainLoose(Boolean.parseBoolean(attribute.getValue()));
                    break;
                case KEY_TERRAIN_OTHER_DETAIL:
                    builder.setTerrainOtherDetail(attribute.getValue());
                    break;
                case KEY_TERRAIN_SLOPE_DIR:
                    builder.setTerrainSlopeDir(attribute.getValue());
                    break;
                case KEY_MEDLINE_REMARKS:
                    builder.setMedlineRemarks(attribute.getValue());
                    break;
                case KEY_MARKED_BY:
                    builder.setMarkedBy(attribute.getValue());
                    break;
                case KEY_OBSTACLES:
                    builder.setObstacles(attribute.getValue());
                    break;
                case KEY_WINDS_ARE_FROM:
                    builder.setWindsAreFrom(attribute.getValue());
                    break;
                case KEY_FRIENDLIES:
                    builder.setFriendlies(attribute.getValue());
                    break;
                case KEY_ENEMY:
                    builder.setEnemy(attribute.getValue());
                    break;
                case KEY_HLZ_REMARKS:
                    builder.setHlzRemarks(attribute.getValue());
                    break;
                case KEY_HLZ_OTHER:
                    builder.setHlzOther(attribute.getValue());
                case KEY_CASEVAC:
                    builder.setCasevac(Boolean.parseBoolean(attribute.getValue()));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: medevac." + attribute.getName());
            }
        }

        for (CotDetail child : cotDetail.getChildren()) {
            switch (child.getElementName()) {
                case KEY_MISTS_MAP:
                    builder.setZMistsMap(mMistsMapProtobufConverter.toMistsMap(child));
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle child detail object: medevac." + child.getElementName());
            }
        }

        return builder.build();
    }

    public void maybeAddMedevac(CotDetail cotDetail, ProtobufMedevac.Medevac medevac) {
        if (medevac == null || medevac == ProtobufMedevac.Medevac.getDefaultInstance()) {
            return;
        }

        CotDetail medevacDetail = new CotDetail(KEY_MEDEVAC);

        medevacDetail.setAttribute(KEY_TITLE, medevac.getTitle());
        medevacDetail.setAttribute(KEY_ZONE_PROT_SELECTION, Integer.toString(medevac.getZoneProtSelection()));
        medevacDetail.setAttribute(KEY_FREQUENCY, medevac.getFreq());
        medevacDetail.setAttribute(KEY_URGENT, Integer.toString(medevac.getUrgent()));
        medevacDetail.setAttribute(KEY_PRIORITY, Integer.toString(medevac.getPriority()));
        medevacDetail.setAttribute(KEY_ROUTINE, Integer.toString(medevac.getRoutine()));
        medevacDetail.setAttribute(KEY_HOIST, Boolean.toString(medevac.getHoist()));
        medevacDetail.setAttribute(KEY_VENTILATOR, Boolean.toString(medevac.getVentilator()));
        medevacDetail.setAttribute(KEY_EQUIPMENT_OTHER, Boolean.toString(medevac.getEquipmentOther()));
        medevacDetail.setAttribute(KEY_EXTRACTION_EQUIPMENT, Boolean.toString(medevac.getExtractionEquipment()));
        medevacDetail.setAttribute(KEY_EQUIPMENT_NONE, Boolean.toString(medevac.getEquipmentNone()));
        medevacDetail.setAttribute(KEY_EQUIPMENT_DETAIL, medevac.getEquipmentDetail());
        medevacDetail.setAttribute(KEY_LITTER, Integer.toString(medevac.getLitter()));
        medevacDetail.setAttribute(KEY_AMBULATORY, Integer.toString(medevac.getAmbulatory()));
        medevacDetail.setAttribute(KEY_SECURITY, Integer.toString(medevac.getSecurity()));
        medevacDetail.setAttribute(KEY_HLZ_MARKING, Integer.toString(medevac.getHlzMarking()));
        medevacDetail.setAttribute(KEY_NONUS_CIVILIAN, Integer.toString(medevac.getNonusCivilian()));
        medevacDetail.setAttribute(KEY_US_CIVILIAN, Integer.toString(medevac.getUsCivilian()));
        medevacDetail.setAttribute(KEY_CHILD, Integer.toString(medevac.getChild()));
        medevacDetail.setAttribute(KEY_EPW, Integer.toString(medevac.getEpw()));
        medevacDetail.setAttribute(KEY_US_MILITARY, Integer.toString(medevac.getUsMilitary()));
        medevacDetail.setAttribute(KEY_NONUS_MILITARY, Integer.toString(medevac.getNonusMilitary()));
        medevacDetail.setAttribute(KEY_TERRAIN_SLOPE, Boolean.toString(medevac.getTerrainSlope()));
        medevacDetail.setAttribute(KEY_TERRAIN_ROUGH, Boolean.toString(medevac.getTerrainRough()));
        medevacDetail.setAttribute(KEY_TERRAIN_NONE, Boolean.toString(medevac.getTerrainNone()));
        medevacDetail.setAttribute(KEY_TERRAIN_OTHER, Boolean.toString(medevac.getTerrainOther()));
        medevacDetail.setAttribute(KEY_TERRAIN_LOOSE, Boolean.toString(medevac.getTerrainLoose()));
        medevacDetail.setAttribute(KEY_TERRAIN_OTHER_DETAIL, medevac.getTerrainOtherDetail());
        medevacDetail.setAttribute(KEY_TERRAIN_SLOPE_DIR, medevac.getTerrainSlopeDir());
        medevacDetail.setAttribute(KEY_MEDLINE_REMARKS, medevac.getMedlineRemarks());
        medevacDetail.setAttribute(KEY_MARKED_BY, medevac.getMarkedBy());
        medevacDetail.setAttribute(KEY_OBSTACLES, medevac.getObstacles());
        medevacDetail.setAttribute(KEY_WINDS_ARE_FROM, medevac.getWindsAreFrom());
        medevacDetail.setAttribute(KEY_FRIENDLIES, medevac.getFriendlies());
        medevacDetail.setAttribute(KEY_ENEMY, medevac.getEnemy());
        medevacDetail.setAttribute(KEY_HLZ_REMARKS, medevac.getHlzRemarks());
        medevacDetail.setAttribute(KEY_HLZ_OTHER, medevac.getHlzOther());
        medevacDetail.setAttribute(KEY_CASEVAC, Boolean.toString(medevac.getCasevac()));

        mMistsMapProtobufConverter.maybeAddMistsMap(medevacDetail, medevac.getZMistsMap());

        cotDetail.addChild(medevacDetail);
    }
}

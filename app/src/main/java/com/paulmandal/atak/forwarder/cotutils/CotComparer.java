package com.paulmandal.atak.forwarder.cotutils;


import androidx.annotation.Nullable;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;

import java.util.List;

public class CotComparer {
    public boolean areCotEventsEqual(CotEvent lhs, CotEvent rhs) {
        if (!areCotDetailsEqual(lhs.getDetail(), rhs.getDetail())) {
            return false;
        }

        if (!areNullableStringsEqual(lhs.getAccess(), rhs.getAccess())) {
            return false;
        }

        if (!areCotPointsEqual(lhs.getCotPoint(), rhs.getCotPoint())) {
            return false;
        }

        if (!areNullableStringsEqual(lhs.getHow(), rhs.getHow())) {
            return false;
        }

        if (!areNullableStringsEqual(lhs.getOpex(), (rhs.getOpex()))) {
            return false;
        }

        if (!areNullableStringsEqual(lhs.getQos(), (rhs.getQos()))) {
            return false;
        }

        if (!areNullableStringsEqual(lhs.getType(), (rhs.getType()))) {
            return false;
        }

        if (!areNullableStringsEqual(lhs.getUID(), (rhs.getUID()))) {
            return false;
        }

        if (!areNullableStringsEqual(lhs.getVersion(), (rhs.getVersion()))) {
            return false;
        }

        return true;
    }

    private boolean areCotDetailsEqual(CotDetail lhs, CotDetail rhs) {
        if (!areNullableStringsEqual(lhs.getElementName(), rhs.getElementName())) {
            return false;
        }

        if (!areNullableStringsEqual(lhs.getInnerText(), rhs.getInnerText())) {
            return false;
        }

        CotAttribute[] lhsCotAttributes = lhs.getAttributes();
        CotAttribute[] rhsCotAttributes = rhs.getAttributes();

        if (lhsCotAttributes.length != rhsCotAttributes.length) {
            return false;
        }

        for (int i = 0; i < lhsCotAttributes.length; i++) {
            CotAttribute lhsCotAttribute = lhsCotAttributes[i];
            CotAttribute rhsCotAttribute = rhsCotAttributes[i];

            if (!areCotAttributesEqual(lhsCotAttribute, rhsCotAttribute)) {
                return false;
            }
        }

        List<CotDetail> lhsChildren = lhs.getChildren();
        List<CotDetail> rhsChildren = rhs.getChildren();

        if (lhsChildren.size() != rhsChildren.size()) {
            return false;
        }

        for (int i = 0; i < lhsChildren.size(); i++) {
            CotDetail lhsChild = lhsChildren.get(i);
            CotDetail rhsChild = rhsChildren.get(i);

            if (!areCotDetailsEqual(lhsChild, rhsChild)) {
                return false;
            }
        }

        return true;
    }

    private boolean areCotAttributesEqual(CotAttribute lhs, CotAttribute rhs) {
        if (!areNullableStringsEqual(lhs.getName(), rhs.getName())) {
            return false;
        }

        if (!areNullableStringsEqual(lhs.getValue(), rhs.getValue())) {
            return false;
        }

        return true;
    }

    private boolean areCotPointsEqual(CotPoint lhs, CotPoint rhs) {
        if (lhs.getCe() != rhs.getCe()) {
            return false;
        }

        if (lhs.getHae() != rhs.getHae()) {
            return false;
        }

        if (lhs.getLat() != rhs.getLat()) {
            return false;
        }

        if (lhs.getLon() != rhs.getLon()) {
            return false;
        }

        if (lhs.getLe() != rhs.getLe()) {
            return false;
        }

        return true;
    }

    private boolean areNullableStringsEqual(@Nullable String lhs, @Nullable String rhs) {
        if (lhs == null && rhs == null) {
            return true;
        }

        if (lhs != null && !lhs.equals(rhs)) {
            return false;
        }

        if (!rhs.equals(lhs)) {
            return false;
        }

        return true;
    }

    public boolean areUidsEqual(String[] lhs, String[] rhs) {
        if (lhs == null && rhs == null) {
            return true;
        }

        if (lhs != null && rhs == null) {
            return false;
        }

        if (lhs == null) {
            return false;
        }

        if (lhs.length != rhs.length) {
            return false;
        }

        for (int i = 0; i < lhs.length; i++) {
            if (!areNullableStringsEqual(lhs[i], rhs[i])) {
                return false;
            }
        }

        return true;
    }
}

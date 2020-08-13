package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;
import com.siemens.ct.exi.core.EXIFactory;
import com.siemens.ct.exi.core.exceptions.EXIException;
import com.siemens.ct.exi.main.api.sax.EXIResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class OutboundMessageHandler implements CommsMapComponent.PreSendProcessor {
    private static final String TAG = "ATAKDBG." + OutboundMessageHandler.class.getSimpleName();

    private CommsMapComponent mCommsMapComponent;
    private CommHardware mCommHardware;
    private EXIFactory mExiFactory;

    public OutboundMessageHandler(CommsMapComponent commsMapComponent, CommHardware commHardware, EXIFactory exiFactory) {
        mCommsMapComponent = commsMapComponent;
        mCommHardware = commHardware;
        mExiFactory = exiFactory;

        commsMapComponent.registerPreSendProcessor(this);
    }

    public void destroy() {
        mCommsMapComponent.registerPreSendProcessor(null);
    }

    @Override
    public void processCotEvent(CotEvent cotEvent, String[] toUIDs) {
        // TODO: handle toUIDs here
        String cotString = cotEvent.toString();
        byte[] exiBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ByteArrayInputStream bais = new ByteArrayInputStream(cotString.getBytes())) {
            EXIResult exiResult = new EXIResult(mExiFactory);
            exiResult.setOutputStream(baos);
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser newSAXParser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = newSAXParser.getXMLReader();
            xmlReader.setContentHandler(exiResult.getHandler());
            InputSource inputSource = new InputSource(bais);
            xmlReader.parse(inputSource);
            exiBytes = baos.toByteArray();
        } catch (IOException | EXIException | SAXException | ParserConfigurationException e) {
            e.printStackTrace();
            return;
        }

        Log.d(TAG, "processCotEvent(): length: " + cotString.length() + ", EXI length: " + exiBytes.length + ", to UIDs: " + Arrays.toString(toUIDs));
        mCommHardware.sendMessage(exiBytes, toUIDs);
    }
}

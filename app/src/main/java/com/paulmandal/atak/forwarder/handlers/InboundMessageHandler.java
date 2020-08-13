package com.paulmandal.atak.forwarder.handlers;

import android.util.Log;

import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.interfaces.CommHardware;
import com.siemens.ct.exi.core.EXIFactory;
import com.siemens.ct.exi.core.exceptions.EXIException;
import com.siemens.ct.exi.main.api.sax.EXISource;

import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

public class InboundMessageHandler implements CommHardware.Listener {
    private static final String TAG = "ATAKDBG." + InboundMessageHandler.class.getSimpleName();

    private static final int INBOUND_MESSAGE_DEST_PORT = Config.INBOUND_MESSAGE_DEST_PORT;
    private static final int INBOUND_MESSAGE_SRC_PORT = Config.INBOUND_MESSAGE_SRC_PORT;

    private CommHardware mCommHardware;
    private EXIFactory mExiFactory;

    public InboundMessageHandler(CommHardware commHardware, EXIFactory exiFactory) {
        mCommHardware = commHardware;
        mExiFactory = exiFactory;

        commHardware.addListener(this);
    }

    @Override
    public void onMessageReceived(byte[] message) {
        new Thread(() -> {
            String cotEventString;
            byte[] cotEventBytes;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(message);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                InputSource is = new InputSource(bais);
                SAXSource exiSource = new EXISource(mExiFactory);
                exiSource.setInputSource(is);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();

                Result result = new StreamResult(baos);
                transformer.transform(exiSource, result);
                cotEventString = baos.toString();
                cotEventBytes = baos.toByteArray();
            } catch (IOException | EXIException | TransformerException e) {
                e.printStackTrace();
                return;
            }

            Log.d(TAG, "onMessageReceived(): " + cotEventString);
            try (DatagramSocket socket = new DatagramSocket(INBOUND_MESSAGE_SRC_PORT)) {
                InetAddress serverAddr = InetAddress.getLocalHost();
                DatagramPacket packet = new DatagramPacket(cotEventBytes, cotEventBytes.length, serverAddr, INBOUND_MESSAGE_DEST_PORT);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IOException while trying to send message to UDP");
            }
        }).start();
    }
}

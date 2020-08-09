package com.paulmandal.atak.gotenna.mesh.models;

import android.util.Log;

import com.gotenna.sdk.data.messages.GTTextOnlyMessageData;
import com.gotenna.sdk.exceptions.GTDataMissingException;

import java.util.Date;

public class Message {
    //==============================================================================================
    // Class Properties
    //==============================================================================================

    private static final String LOG_TAG = "Message";

    private long senderGID;
    private long receiverGID;
    private Date sentDate;
    private String text;
    private MessageStatus messageStatus;
    private String detailInfo;
    private int hopCount;

    public enum MessageStatus
    {
        SENDING,
        SENT_SUCCESSFULLY,
        ERROR_SENDING
    }

    //==============================================================================================
    // Constructor
    //==============================================================================================

    public Message(long senderGID, long receiverGID, Date sentDate, String text, MessageStatus messageStatus, String detailInfo)
    {
        this.senderGID = senderGID;
        this.receiverGID = receiverGID;
        this.sentDate = sentDate;
        this.text = text;
        this.messageStatus = messageStatus;
        this.detailInfo = detailInfo;
    }

    //==============================================================================================
    // Class Instance Methods
    //==============================================================================================

    public long getSenderGID()
    {
        return senderGID;
    }

    public long getReceiverGID()
    {
        return receiverGID;
    }

    public Date getSentDate()
    {
        return sentDate;
    }

    public String getText()
    {
        return text;
    }

    public MessageStatus getMessageStatus()
    {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus)
    {
        this.messageStatus = messageStatus;
    }

    public String getDetailInfo()
    {
        return detailInfo;
    }

    public byte[] toBytes()
    {
        // Use the goTenna SDK's helper classes to format the text data
        // in a way that is easily parsable
        GTTextOnlyMessageData gtTextOnlyMessageData = null;

        try
        {
            gtTextOnlyMessageData = new GTTextOnlyMessageData(text);
        }
        catch (GTDataMissingException e)
        {
            Log.w(LOG_TAG, e);
        }

        if (gtTextOnlyMessageData == null)
        {
            return null;
        }

        return gtTextOnlyMessageData.serializeToBytes();
    }

    public void setHopCount(int hopCount)
    {
        this.hopCount = hopCount;
    }

    public int getHopCount()
    {
        return hopCount;
    }

    //==============================================================================================
    // Static Helper Methods
    //==============================================================================================

    public static Message createReadyToSendMessage(long senderGID, long receiverGID, String text)
    {
        return new Message(senderGID, receiverGID, new Date(), text, MessageStatus.SENDING, null);
    }

    public static Message createMessageFromData(GTTextOnlyMessageData gtTextOnlyMessageData)
    {
        return new Message(gtTextOnlyMessageData.getSenderGID(),
                gtTextOnlyMessageData.getRecipientGID(),
                gtTextOnlyMessageData.getMessageSentDate(),
                gtTextOnlyMessageData.getText(),
                MessageStatus.SENT_SUCCESSFULLY,
                "");
    }
}
